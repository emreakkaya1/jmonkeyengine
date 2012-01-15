/*
 *  Copyright (c) 2009-2010 jMonkeyEngine
 *  All rights reserved.
 * 
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are
 *  met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 *  * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jme3tools.optimize;

import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.material.MatParamTexture;
import com.jme3.material.Material;
import com.jme3.math.Vector2f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author Lukasz Bruun - lukasz.dk, normenhansen
 */
public class TextureAtlas {

    private Map<String, byte[]> images;
    private int atlasWidth, atlasHeight;
    private Format format = Format.ABGR8;
    private Node root;
    private Map<String, TextureAtlasTile> locationMap;
    private String rootMapName;

    public TextureAtlas(int width, int height) {
        this.atlasWidth = width;
        this.atlasHeight = height;
        root = new Node(0, 0, width, height);
        locationMap = new TreeMap<String, TextureAtlasTile>();
    }

    /**
     * Add a texture for a specific map name
     * @param texture A texture to add to the atlas
     * @param mapName A freely chosen map name that can be later retrieved as a Texture. The first map name supplied will be the master map.
     * @return false If texture cannot be added to atlas because it does not fit
     */
    public boolean addTexture(Texture texture, String mapName) {
        if (texture == null) {
            throw new IllegalStateException("Texture cannot be null");
        }
        String name = textureName(texture);
        if (texture.getImage() != null && name != null) {
            return addImage(texture.getImage(), name, mapName, null);
        } else {
            throw new IllegalStateException("Source texture has no asset name");
        }
    }

    /**
     * Add a texture for a specific map name at the location of another existing texture (on the master map).
     * @param texture A texture to add to the atlas.
     * @param mapName A freely chosen map name that can be later retrieved as a Texture.
     * @param sourceTexture The base texture for determining the location.
     * @return false If texture cannot be added to atlas because it does not fit
     */
    public void addTexture(Texture texture, String mapName, Texture sourceTexture) {
        String sourceTextureName = textureName(sourceTexture);
        if (sourceTextureName == null) {
            throw new IllegalStateException("Source texture has no asset name");
        } else {
            addTexture(texture, mapName, sourceTextureName);
        }
    }

    /**
     * Add a texture for a specific map name at the location of another existing texture (on the master map).
     * @param texture A texture to add to the atlas.
     * @param mapName A freely chosen map name that can be later retrieved as a Texture.
     * @param sourceTextureName Name of the base texture for the location.
     * @return false If texture cannot be added to atlas because it does not fit
     */
    public void addTexture(Texture texture, String mapName, String sourceTextureName) {
        if (texture == null) {
            throw new IllegalStateException("Texture cannot be null");
        }
        String name = textureName(texture);
        if (texture.getImage() != null && name != null) {
            addImage(texture.getImage(), name, mapName, sourceTextureName);
        } else {
            throw new IllegalStateException("Texture has no asset name");
        }
    }

    private String textureName(Texture texture) {
        if (texture == null) {
            return null;
        }
        AssetKey key = texture.getKey();
        if (key != null) {
            return key.getName();
        } else {
            return null;
        }
    }

    private boolean addImage(Image image, String name, String mapName, String sourceTextureName) {
        if (rootMapName == null) {
            rootMapName = mapName;
        }
        if (sourceTextureName == null && !rootMapName.equals(mapName)) {
            throw new IllegalStateException("Cannot add texture " + name + " to new map without source texture.");
        }
        TextureAtlasTile location = locationMap.get(name);
        if (location != null) {
            locationMap.put(name, location);
            return true;
        } else if (sourceTextureName == null) {
            Node node = root.insert(image);
            if (node == null) {
                return false;
            }
            location = node.location;
        } else {
            location = locationMap.get(sourceTextureName);
            if (location == null) {
                throw new IllegalStateException("Cannot find source texture for " + name + ".");
            } else if (location.width != image.getWidth() || location.height != image.getHeight()) {
                throw new IllegalStateException("Secondary texture " + name + " does not fit main texture size.");
            }
        }
        locationMap.put(name, location);
        drawImage(image, location.getX(), location.getY(), mapName);
        return true;
    }

    private void drawImage(Image source, int x, int y, String mapName) {
        if (images == null) {
            images = new HashMap<String, byte[]>();
        }
        byte[] image = images.get(mapName);
        if (image == null) {
            image = new byte[atlasWidth * atlasHeight * 4];
            images.put(mapName, image);
        }
        //TODO: all buffers?
        ByteBuffer sourceData = source.getData(0);
        int height = source.getHeight();
        int width = source.getWidth();
        for (int yPos = 0; yPos < height; yPos++) {
            for (int xPos = 0; xPos < width; xPos++) {
                int i = ((xPos + x) + (yPos + y) * atlasWidth) * 4;
                if (source.getFormat() == Format.ABGR8) {
                    int j = (xPos + yPos * width) * 4;
                    image[i] = sourceData.get(j); //a
                    image[i + 1] = sourceData.get(j + 1); //b
                    image[i + 2] = sourceData.get(j + 2); //g
                    image[i + 3] = sourceData.get(j + 3); //r
                } else if (source.getFormat() == Format.BGR8) {
                    int j = (xPos + yPos * width) * 3;
                    image[i] = 1; //a
                    image[i + 1] = sourceData.get(j); //b
                    image[i + 2] = sourceData.get(j + 1); //g
                    image[i + 3] = sourceData.get(j + 2); //r
                } else if (source.getFormat() == Format.RGB8) {
                    int j = (xPos + yPos * width) * 3;
                    image[i] = 1; //a
                    image[i + 1] = sourceData.get(j + 2); //b
                    image[i + 2] = sourceData.get(j + 1); //g
                    image[i + 3] = sourceData.get(j); //r
                } else if (source.getFormat() == Format.RGBA8) {
                    int j = (xPos + yPos * width) * 4;
                    image[i] = sourceData.get(j + 3); //a
                    image[i + 1] = sourceData.get(j + 2); //b
                    image[i + 2] = sourceData.get(j + 1); //g
                    image[i + 3] = sourceData.get(j); //r
                } else {
                    throw new UnsupportedOperationException("Could not draw texture with format " + source.getFormat());
                }
            }
        }
    }

    /**
     * Get the <code>TextureAtlasTile</code> for the given Texture
     * @param texture The texture to retrieve the <code>TextureAtlasTile</code> for.
     * @return 
     */
    public TextureAtlasTile getAtlasTile(Texture texture) {
        String sourceTextureName = textureName(texture);
        if (sourceTextureName != null) {
            return getAtlasTile(sourceTextureName);
        }
        return null;
    }

    /**
     * Get the <code>TextureAtlasTile</code> for the given Texture
     * @param assetName The texture to retrieve the <code>TextureAtlasTile</code> for.
     * @return 
     */
    private TextureAtlasTile getAtlasTile(String assetName) {
        return locationMap.get(assetName);
    }

    /**
     * Gets a new atlas texture for the given map name.
     * @param mapName
     * @return 
     */
    public Texture getAtlasTexture(String mapName) {
        if (images == null) {
            return null;
        }
        byte[] image = images.get(mapName);
        if (image != null) {
            Texture2D tex = new Texture2D(new Image(format, atlasWidth, atlasHeight, BufferUtils.createByteBuffer(image)));
            tex.setMagFilter(Texture.MagFilter.Bilinear);
            tex.setMinFilter(Texture.MinFilter.BilinearNearestMipMap);
            tex.setWrap(Texture.WrapMode.Clamp);
            return tex;
        }
        return null;
    }

    /**
     * Applies the texture coordinates to the given geometry
     * if its DiffuseMap or ColorMap exists in the atlas.
     * @param geom The geometry to change the texture coordinate buffer on.
     * @return true if texture has been found and coords have been changed, false otherwise
     */
    public boolean applyCoords(Geometry geom) {
        return applyCoords(geom, 0, geom.getMesh());
    }

    /**
     * Applies the texture coordinates to the given output mesh
     * if the DiffuseMap or ColorMap of the input geometry exist in the atlas.
     * @param geom The geometry to change the texture coordinate buffer on.
     * @param offset Target buffer offset
     * @param outMesh The mesh to set the coords in (can be same as input)
     * @return true if texture has been found and coords have been changed, false otherwise
     */
    public boolean applyCoords(Geometry geom, int offset, Mesh outMesh) {
        Mesh inMesh = geom.getMesh();
        geom.computeWorldMatrix();

        VertexBuffer inBuf = inMesh.getBuffer(Type.TexCoord);
        VertexBuffer outBuf = outMesh.getBuffer(Type.TexCoord);

        if (inBuf == null || outBuf == null) {
            throw new IllegalStateException("Geometry mesh has no texture coordinate buffer.");
        }

        Texture tex = getMaterialTexture(geom, "DiffuseMap");
        if (tex == null) {
            tex = getMaterialTexture(geom, "ColorMap");

        }
        if (tex != null) {
            TextureAtlasTile tile = getAtlasTile(tex);
            if (tile != null) {
                FloatBuffer inPos = (FloatBuffer) inBuf.getData();
                FloatBuffer outPos = (FloatBuffer) outBuf.getData();
                tile.transformTextureCoords(inPos, offset, outPos);
                return true;
            } else {
                return false;
            }
        } else {
            throw new IllegalStateException("Geometry has no proper texture.");
        }
    }

    /**
     * Create a texture atlas for the given root node, containing DiffuseMap, NormalMap and SpecularMap.
     * @param root The rootNode to create the atlas for
     * @param atlasSize The size of the atlas (width and height)
     * @return Null if the atlas cannot be created because not all textures fit
     */
    public static TextureAtlas createAtlas(Spatial root, int atlasSize) {
        List<Geometry> geometries = new ArrayList<Geometry>();
        GeometryBatchFactory.gatherGeoms(root, geometries);
        TextureAtlas atlas = new TextureAtlas(atlasSize, atlasSize);
        for (Geometry geometry : geometries) {
            Texture diffuse = getMaterialTexture(geometry, "DiffuseMap");
            Texture normal = getMaterialTexture(geometry, "NormalMap");
            Texture specular = getMaterialTexture(geometry, "SpecularMap");
            if (diffuse == null) {
                diffuse = getMaterialTexture(geometry, "ColorMap");

            }
            if (diffuse != null && diffuse.getKey() != null) {
                String keyName = diffuse.getKey().getName();
                if (!atlas.addTexture(diffuse, "DiffuseMap")) {
                    return null;
                } else {
                    if (normal != null && normal.getKey() != null) {
                        atlas.addTexture(diffuse, "NormalMap", keyName);
                    }
                    if (specular != null && specular.getKey() != null) {
                        atlas.addTexture(specular, "SpecularMap", keyName);
                    }
                }
            }
        }
        return atlas;
    }

    /**
     * Creates one geometry out of the given root spatial and merges all single
     * textures into one texture of the given size.
     * @param spat The root spatial of the scene to batch
     * @param mgr An assetmanager that can be used to create the material
     * @param atlasSize A size for the atlas texture, it has to be large enough to hold all single textures
     * @return A new geometry that uses the generated texture atlas and merges all meshes of the root spatial, null if the atlas cannot be created because not all textures fit
     */
    public static Geometry makeAtlasBatch(Spatial spat, AssetManager mgr, int atlasSize) {
        List<Geometry> geometries = new ArrayList<Geometry>();
        GeometryBatchFactory.gatherGeoms(spat, geometries);
        TextureAtlas atlas = createAtlas(spat, atlasSize);
        if (atlas == null) {
            return null;
        }
        Geometry geom = new Geometry();
        Mesh mesh = new Mesh();
        GeometryBatchFactory.mergeGeometries(geometries, mesh);
        applyAtlasCoords(geometries, mesh, atlas);
        mesh.updateCounts();
        mesh.updateBound();
        geom.setMesh(mesh);

        Material mat = new Material(mgr, "Common/MatDefs/Light/Lighting.j3md");
        mat.getAdditionalRenderState().setAlphaTest(true);
        Texture diffuseMap = atlas.getAtlasTexture("DiffuseMap");
        Texture normalMap = atlas.getAtlasTexture("NormalMap");
        Texture specularMap = atlas.getAtlasTexture("SpecularMap");
        if (diffuseMap != null) {
            mat.setTexture("DiffuseMap", diffuseMap);
        }
        if (normalMap != null) {
            mat.setTexture("NormalMap", normalMap);
        }
        if (specularMap != null) {
            mat.setTexture("SpecularMap", specularMap);
        }
        mat.setFloat("Shininess", 16.0f);

        geom.setMaterial(mat);
        return geom;
    }

    private static void applyAtlasCoords(List<Geometry> geometries, Mesh outMesh, TextureAtlas atlas) {
        int globalVertIndex = 0;

        for (Geometry geom : geometries) {
            Mesh inMesh = geom.getMesh();
            geom.computeWorldMatrix();

            int geomVertCount = inMesh.getVertexCount();

            VertexBuffer inBuf = inMesh.getBuffer(Type.TexCoord);
            VertexBuffer outBuf = outMesh.getBuffer(Type.TexCoord);

            if (inBuf == null || outBuf == null) {
                continue;
            }

            atlas.applyCoords(geom, globalVertIndex, outMesh);

            globalVertIndex += geomVertCount;
        }
    }

    private static Texture getMaterialTexture(Geometry geometry, String mapName) {
        Material mat = geometry.getMaterial();
        if (mat == null || mat.getParam(mapName) == null || !(mat.getParam(mapName) instanceof MatParamTexture)) {
            return null;
        }
        MatParamTexture param = (MatParamTexture) mat.getParam(mapName);
        Texture texture = param.getTextureValue();
        if (texture == null) {
            return null;
        }
        return texture;


    }

    private class Node {

        public TextureAtlasTile location;
        public Node child[];
        public Image image;

        public Node(int x, int y, int width, int height) {
            location = new TextureAtlasTile(x, y, width, height);
            child = new Node[2];
            child[0] = null;
            child[1] = null;
            image = null;
        }

        public boolean isLeaf() {
            return child[0] == null && child[1] == null;
        }

        // Algorithm from http://www.blackpawn.com/texts/lightmaps/
        public Node insert(Image image) {
            if (!isLeaf()) {
                Node newNode = child[0].insert(image);

                if (newNode != null) {
                    return newNode;
                }

                return child[1].insert(image);
            } else {
                if (this.image != null) {
                    return null; // occupied
                }

                if (image.getWidth() > location.getWidth() || image.getHeight() > location.getHeight()) {
                    return null; // does not fit
                }

                if (image.getWidth() == location.getWidth() && image.getHeight() == location.getHeight()) {
                    this.image = image; // perfect fit
                    return this;
                }

                int dw = location.getWidth() - image.getWidth();
                int dh = location.getHeight() - image.getHeight();

                if (dw > dh) {
                    child[0] = new Node(location.getX(), location.getY(), image.getWidth(), location.getHeight());
                    child[1] = new Node(location.getX() + image.getWidth(), location.getY(), location.getWidth() - image.getWidth(), location.getHeight());
                } else {
                    child[0] = new Node(location.getX(), location.getY(), location.getWidth(), image.getHeight());
                    child[1] = new Node(location.getX(), location.getY() + image.getHeight(), location.getWidth(), location.getHeight() - image.getHeight());
                }

                return child[0].insert(image);
            }
        }
    }

    public class TextureAtlasTile {

        private int x;
        private int y;
        private int width;
        private int height;

        public TextureAtlasTile(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        /**
         * Get the transformed texture location for a given input location
         * @param previousLocation
         * @return 
         */
        public Vector2f getLocation(Vector2f previousLocation) {
            float x = (float) getX() / (float) atlasWidth;
            float y = (float) getY() / (float) atlasHeight;
            float w = (float) getWidth() / (float) atlasWidth;
            float h = (float) getHeight() / (float) atlasHeight;
            Vector2f location = new Vector2f(x, y);
            float prevX = previousLocation.x;
            float prevY = previousLocation.y;
            location.addLocal(prevX * w, prevY * h);
            return location;
        }

        /**
         * Transforms a whole texture coordinates buffer
         * @param inBuf The input texture buffer
         * @param offset The offset in the output buffer
         * @param outBuf The output buffer
         */
        public void transformTextureCoords(FloatBuffer inBuf, int offset, FloatBuffer outBuf) {
            Vector2f tex = new Vector2f();

            // offset is given in element units
            // convert to be in component units
            offset *= 2;

            for (int i = 0; i < inBuf.capacity() / 2; i++) {
                tex.x = inBuf.get(i * 2 + 0);
                tex.y = inBuf.get(i * 2 + 1);
                Vector2f location = getLocation(tex);
                //TODO: replace with proper texture wrapping for atlases..
                outBuf.put(offset + i * 2 + 0, location.x);
                outBuf.put(offset + i * 2 + 1, location.y);
            }
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }
}