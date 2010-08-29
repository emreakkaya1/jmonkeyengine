package com.jme3.niftygui;

import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix4f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.scene.Geometry;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Format;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.util.BufferUtils;
import de.lessvoid.nifty.elements.render.TextRenderer.RenderFontNull;
import de.lessvoid.nifty.render.BlendMode;
import de.lessvoid.nifty.spi.render.RenderDevice;
import de.lessvoid.nifty.spi.render.RenderFont;
import de.lessvoid.nifty.spi.render.RenderImage;
import de.lessvoid.nifty.tools.Color;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class RenderDeviceJme implements RenderDevice {

    private NiftyJmeDisplay display;
    private RenderManager rm;
    private Renderer r;

    private final Quad quad = new Quad(1, -1, true);
    private final Geometry quadGeom = new Geometry("nifty-quad", quad);
    private final Geometry gradQuadGeom = new Geometry("nifty-gradient-quad", quad);
    private final Geometry imageQuadGeom = new Geometry("nifty-image-quad", quad);
//    private final Node imageQuadNode = new Node("nifty-image-node");
//    private final Node textNode = new Node("nifty-text-node");

    private final ColorRGBA colorRgba = new ColorRGBA();
    private final Material niftyMat;

    private boolean clipWasSet = false;
    private BlendMode blendMode = null;

    private VertexBuffer quadDefaultTC = quad.getBuffer(Type.TexCoord);
    private VertexBuffer quadModTC = quadDefaultTC.clone();

    private Matrix4f tempMat = new Matrix4f();

    public RenderDeviceJme(NiftyJmeDisplay display){
        this.display = display;

        VertexBuffer vb = new VertexBuffer(Type.Color);
        vb.setNormalized(true);
        ByteBuffer bb = BufferUtils.createByteBuffer(4 * 4);
        vb.setupData(Usage.Stream, 4, Format.UnsignedByte, bb);
        quad.setBuffer(vb);

        quadModTC.setUsage(Usage.Stream);

        niftyMat = new Material(display.getAssetManager(), "Common/MatDefs/Nifty/Nifty.j3md");
    }

    public void setRenderManager(RenderManager rm){
        this.rm = rm;
        this.r = rm.getRenderer();
    }

    public RenderImage createImage(String filename, boolean linear) {
        return new RenderImageJme(filename, linear, display);
    }

    public RenderFont createFont(String filename) {
        return new RenderFontJme(filename, display);
    }

    public void beginFrame() {
    }

    public void endFrame() {
    }

    public int getWidth() {
        return display.getWidth();
    }

    public int getHeight() {
        return display.getHeight();
    }

    public void clear() {
    }

    public void setBlendMode(BlendMode blendMode) {
        if (this.blendMode != blendMode){
            this.blendMode = blendMode;
        }
    }

    private RenderState.BlendMode convertBlend(){
        if (blendMode == null)
            return RenderState.BlendMode.Off;
        else if (blendMode == BlendMode.BLEND)
            return RenderState.BlendMode.Alpha;
        else if (blendMode == BlendMode.MULIPLY)
            return RenderState.BlendMode.Modulate;
        else
            throw new UnsupportedOperationException();
    }

    private ColorRGBA convertColor(Color color){
        if (color == null)
            colorRgba.set(ColorRGBA.White);
        else
            colorRgba.set(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        
        return colorRgba;
    }

    public void renderFont(RenderFont font, String str, int x, int y, Color color, float size){
        if (str.length() == 0)
            return;

        if (font instanceof RenderFontNull)
            return;

        RenderFontJme jmeFont = (RenderFontJme) font;
        Texture2D texture = jmeFont.getTexture();
        BitmapText text = jmeFont.getText();

        niftyMat.setColor("m_Color", convertColor(color));
        niftyMat.setTexture("m_Texture", texture);
        niftyMat.setInt("m_Mode", 4);
        niftyMat.getAdditionalRenderState().setBlendMode(convertBlend());

        text.setText(str);
        text.updateLogicalState(0);

        float width = text.getLineWidth();
        float height = text.getLineHeight();

        float x0 = x + 0.5f * width  * (1f - size);
        float y0 = y + 0.5f * height * (1f - size);

        tempMat.loadIdentity();
        tempMat.setTranslation(x0, getHeight() - y0, 0);
        tempMat.setScale(size, size, 0);

        rm.setWorldMatrix(tempMat);
        niftyMat.render(text, rm);
    }

    public void renderImage(RenderImage image, int x, int y, int w, int h,
                            int srcX, int srcY, int srcW, int srcH,
                            Color color, float scale,
                            int centerX, int centerY){

        RenderImageJme jmeImage = (RenderImageJme) image;
        Texture2D texture = jmeImage.getTexture();

        niftyMat.getAdditionalRenderState().setBlendMode(convertBlend());
        niftyMat.setTexture("m_Texture", texture);
        niftyMat.setInt("m_Mode", 3);
        niftyMat.setColor("m_Color", convertColor(color));

        float imageWidth  = texture.getImage().getWidth();
        float imageHeight = texture.getImage().getHeight();
        FloatBuffer texCoords = (FloatBuffer) quadModTC.getData();

        float startX = srcX / imageWidth;
        float startY = srcY / imageHeight;
        float endX   = startX + (srcW / imageWidth);
        float endY   = startY + (srcH / imageHeight);

        startY = 1f - startY;
        endY   = 1f - endY;

        texCoords.rewind();
        texCoords.put(startX).put(startY);
        texCoords.put(endX)  .put(startY);
        texCoords.put(endX)  .put(endY);
        texCoords.put(startX).put(endY);
        texCoords.flip();
        quadModTC.updateData(texCoords);

        quad.clearBuffer(Type.TexCoord);
        quad.setBuffer(quadModTC);

        float x0 = centerX + (x - centerX) * scale;
        float y0 = centerY + (y - centerY) * scale;

        tempMat.loadIdentity();
        tempMat.setTranslation(x0, getHeight() - y0, 0);
        tempMat.setScale(w * scale, h * scale, 0);

        rm.setWorldMatrix(tempMat);
        niftyMat.render(imageQuadGeom, rm);
    }

    public void renderImage(RenderImage image, int x, int y, int width, int height,
                       Color color, float imageScale){

        RenderImageJme jmeImage = (RenderImageJme) image;

        niftyMat.getAdditionalRenderState().setBlendMode(convertBlend());
        niftyMat.setTexture("m_Texture", jmeImage.getTexture());
        niftyMat.setInt("m_Mode", 3);
        niftyMat.setColor("m_Color", convertColor(color));
          
        quad.clearBuffer(Type.TexCoord);
        quad.setBuffer(quadDefaultTC);

        float x0 = x + 0.5f * width  * (1f - imageScale);
        float y0 = y + 0.5f * height * (1f - imageScale);

        tempMat.loadIdentity();
        tempMat.setTranslation(x0, getHeight() - y0, 0);
        tempMat.setScale(width * imageScale, height * imageScale, 0);

        rm.setWorldMatrix(tempMat);
        niftyMat.render(imageQuadGeom, rm);
    }

    public void renderQuad(int x, int y, int width, int height, Color color){
        niftyMat.getAdditionalRenderState().setBlendMode(convertBlend());
        niftyMat.setInt("m_Mode", 1);
        niftyMat.setColor("m_Color", convertColor(color));

        tempMat.loadIdentity();
        tempMat.setTranslation(x, getHeight() - y, 0);
        tempMat.setScale(width, height, 0);

        rm.setWorldMatrix(tempMat);
        niftyMat.render(quadGeom, rm);
    }

    public void renderQuad(int x, int y, int width, int height,
                           Color topLeft, Color topRight, Color bottomRight, Color bottomLeft) {

        VertexBuffer colors = quad.getBuffer(Type.Color);
        ByteBuffer buf = (ByteBuffer) colors.getData();
        buf.rewind();
        buf.putInt(convertColor(bottomRight).asIntABGR());
        buf.putInt(convertColor(bottomLeft).asIntABGR());
        buf.putInt(convertColor(topLeft).asIntABGR());
        buf.putInt(convertColor(topRight).asIntABGR());
        buf.flip();
        colors.updateData(buf);

        niftyMat.getAdditionalRenderState().setBlendMode(convertBlend());
        niftyMat.setInt("m_Mode", 2);

        tempMat.loadIdentity();
        tempMat.setTranslation(x, getHeight() - y, 0);
        tempMat.setScale(width, height, 0);

        rm.setWorldMatrix(tempMat);
        niftyMat.render(gradQuadGeom, rm);
    }

    public void enableClip(int x0, int y0, int x1, int y1){
        clipWasSet = true;
        r.setClipRect(x0, getHeight() - y1, x1 - x0, y1 - y0);
    }

    public void disableClip() {
        if (clipWasSet){
            r.clearClipRect();
            clipWasSet = false;
        }
    }

}
