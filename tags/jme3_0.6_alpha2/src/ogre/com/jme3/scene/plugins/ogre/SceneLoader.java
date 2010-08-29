package com.jme3.scene.plugins.ogre;

import com.jme3.material.MaterialList;
import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLoader;
import com.jme3.asset.AssetManager;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.PointLight;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.util.xml.SAXUtil;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import org.xml.sax.helpers.XMLReaderFactory;
import static com.jme3.util.xml.SAXUtil.*;

public class SceneLoader extends DefaultHandler implements AssetLoader {

    private static final Logger logger = Logger.getLogger(SceneLoader.class.getName());

    private Stack<String> elementStack = new Stack<String>();
    private String sceneName;
    private String folderName;
    private AssetManager assetManager;
    private MaterialList materialList;
    private Node root;
    private Node node;
    private Node entityNode;
    private Light light;
    private int nodeIdx = 0;
    private static volatile int sceneIdx = 0;

    public SceneLoader(){
        super();
    }

    @Override
    public void startDocument() {
    }

    @Override
    public void endDocument() {
    }

    private Quaternion parseQuat(Attributes attribs) throws SAXException{
        if (attribs.getValue("x") != null){
            // defined as quaternion
            // qx, qy, qz, qw defined
            float x = parseFloat(attribs.getValue("x"));
            float y = parseFloat(attribs.getValue("y"));
            float z = parseFloat(attribs.getValue("z"));
            float w = parseFloat(attribs.getValue("w"));
            return new Quaternion(x,y,z,w);
        }else if (attribs.getValue("qx") != null){
            float x = parseFloat(attribs.getValue("qx"));
            float y = parseFloat(attribs.getValue("qy"));
            float z = parseFloat(attribs.getValue("qz"));
            float w = parseFloat(attribs.getValue("qw"));
            return new Quaternion(x,y,z,w);
        }else if (attribs.getValue("angle") != null){
            // defined as angle + axis
            float angle = parseFloat(attribs.getValue("angle"));
            float axisX = parseFloat(attribs.getValue("axisX"));
            float axisY = parseFloat(attribs.getValue("axisY"));
            float axisZ = parseFloat(attribs.getValue("axisZ"));
            Quaternion q = new Quaternion();
            q.fromAngleAxis(angle, new Vector3f(axisX, axisY, axisZ));
            return q;
        }else{
            float angleX = parseFloat(attribs.getValue("angleX"));
            float angleY = parseFloat(attribs.getValue("angleY"));
            float angleZ = parseFloat(attribs.getValue("angleZ"));
            Quaternion q = new Quaternion();
            q.fromAngles(angleX, angleY, angleZ);
            return q;
        }
    }

    private void parseLightNormal(Attributes attribs) throws SAXException {
        assert elementStack.peek().equals("light");
        
        // SpotLight will be supporting a direction-normal, too.
        if (light instanceof DirectionalLight)
            ((DirectionalLight) light).setDirection(parseVector3(attribs));
    }

    private void parseLightAttenuation(Attributes attribs) throws SAXException {
        // NOTE: Only radius is supported atm ( for pointlights only, since there are no spotlights, yet).
        assert elementStack.peek().equals("light");

        // SpotLight will be supporting a direction-normal, too.
        if (light instanceof PointLight){
            float range = parseFloat(attribs.getValue("range"));
            float constant = parseFloat(attribs.getValue("constant"));
            float linear = parseFloat(attribs.getValue("linear"));

            String quadraticStr = attribs.getValue("quadratic");
            if (quadraticStr == null)
                quadraticStr = attribs.getValue("quadric");

            float quadratic = parseFloat(quadraticStr);
            
            if (constant == 1 && quadratic == 0 && linear > 0){
                range = 1f / linear;
            }
            ((PointLight) light).setRadius(range);
        }

    }

    private void parseLight(Attributes attribs) throws SAXException {
        assert node != null;
        assert node.getParent() != null;
        assert elementStack.peek().equals("node");
        
        String lightType = parseString(attribs.getValue("type"), "point");
        if(lightType.equals("point")) {
            light = new PointLight();
        } else if(lightType.equals("directional")) {
            light = new DirectionalLight();
        } else if(lightType.equals("spotLight")) {
            // TODO: SpotLight class.
            logger.warning("No SpotLight class atm, using Pointlight instead.");
            light = new PointLight();
        } else {
            logger.log(Level.WARNING, "No matching jME3 LightType found for OGRE LightType: {0}", lightType);
        }
        logger.log(Level.FINEST, "{0} created.", light);

        if (!parseBool(attribs.getValue("visible"), true)){
            // set to disabled
        }

        // "attach" it to the parent of this node
        if (light != null)
            node.getParent().addLight(light);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attribs) throws SAXException{
        if (qName.equals("scene")){
            assert elementStack.size() == 0;
            String version = attribs.getValue("formatVersion");
            if (!version.equals("1.0.0"))
                logger.log(Level.WARNING, "Unrecognized version number"
                        + " in dotScene file: {0}", version);
            
        }else if (qName.equals("nodes")){
            assert root == null;
            if (sceneName == null)
                root = new Node("OgreDotScene"+(++sceneIdx));
            else
                root = new Node(sceneName+"-scene_node");
            
            node = root;
        }else if (qName.equals("externals")){
            assert elementStack.peek().equals("scene");

        }else if (qName.equals("item")){
            assert elementStack.peek().equals("externals");
        }else if (qName.equals("file")){
            assert elementStack.peek().equals("item");
            materialList = (MaterialList)
                assetManager.loadAsset(folderName+attribs.getValue("name"));

        }else if (qName.equals("node")){
            String curElement = elementStack.peek();
            assert curElement.equals("nodes") || curElement.equals("node");
            String name = attribs.getValue("name");
            if (name == null)
                name = "OgreNode-" + (++nodeIdx);
            else
                name += "-node";

            Node newNode = new Node(name);
            if (node != null){
                node.attachChild(newNode);
            }
            node = newNode;
        }else if (qName.equals("entity")){
            assert elementStack.peek().equals("node");
            String name = attribs.getValue("name");
            if (name == null)
                name = "OgreEntity-" + (++nodeIdx);
            else
                name += "-entity";

            String meshFile = attribs.getValue("meshFile");
            if (meshFile == null)
                throw new SAXException("Required attribute 'meshFile' missing for 'entity' node");

            // NOTE: append "xml" since its assumed mesh filse are binary in dotScene
            if (folderName != null)
                meshFile = folderName + meshFile;
            
            meshFile += ".xml";
            
            entityNode = new Node(name);
            OgreMeshKey key = new OgreMeshKey(meshFile, materialList);
            Spatial ogreMesh = 
                    (Spatial) assetManager.loadAsset(key);
            //TODO:workaround for meshxml / mesh.xml
            if(ogreMesh==null){
                meshFile = folderName + attribs.getValue("meshFile") + "xml";
                key = new OgreMeshKey(meshFile, materialList);
                ogreMesh = (Spatial) assetManager.loadAsset(key);
            }
            entityNode.attachChild(ogreMesh);
            node.attachChild(entityNode);
            node = null;
        }else if (qName.equals("position")){
            node.setLocalTranslation(SAXUtil.parseVector3(attribs));
        }else if (qName.equals("quaternion") || qName.equals("rotation")){
            node.setLocalRotation(parseQuat(attribs));
        }else if (qName.equals("scale")){
            node.setLocalScale(SAXUtil.parseVector3(attribs));
        } else if (qName.equals("light")) {
            parseLight(attribs);
        } else if (qName.equals("colourDiffuse")) {
            assert elementStack.peek().equals("light");
            light.setColor(parseColor(attribs));
        } else if (qName.equals("normal")) {
            parseLightNormal(attribs);
        } else if (qName.equals("lightAttenuation")) {
            parseLightAttenuation(attribs);
        }

        elementStack.push(qName);
    }

    @Override
    public void endElement(String uri, String name, String qName) {
        if (qName.equals("node")){
            node = node.getParent();
        }else if (qName.equals("nodes")){
            node = null;
        }else if (qName.equals("entity")){
            node = entityNode.getParent();
            entityNode = null;
        }else if (qName.equals("light")){
            // apply the node's world transform on the light..
            root.updateGeometricState();
            if (light instanceof DirectionalLight){
                DirectionalLight dl = (DirectionalLight) light;
                Quaternion q = node.getWorldRotation();
                Vector3f dir = dl.getDirection();
                q.multLocal(dir);
                dl.setDirection(dir);
            }else if (light instanceof PointLight){
                PointLight pl = (PointLight) light;
                Vector3f pos = node.getWorldTranslation();
                pl.setPosition(pos);
            }
            light = null;
        }
        assert elementStack.peek().equals(qName);
        elementStack.pop();
    }

    @Override
    public void characters(char ch[], int start, int length) {
    }

    public Object load(AssetInfo info) throws IOException {
        try{
            assetManager = info.getManager();
            sceneName = info.getKey().getName();
            String ext = info.getKey().getExtension();
            folderName = info.getKey().getFolder();
            sceneName = sceneName.substring(0, sceneName.length() - ext.length() - 1);

            materialList = (MaterialList) 
                    assetManager.loadAsset(new AssetKey(sceneName+".material"));

            XMLReader xr = XMLReaderFactory.createXMLReader();
            xr.setContentHandler(this);
            xr.setErrorHandler(this);
            InputStreamReader r = new InputStreamReader(info.openStream());
            xr.parse(new InputSource(r));
            r.close();
            return root;
        }catch (SAXException ex){
            IOException ioEx = new IOException("Error while parsing Ogre3D dotScene");
            ioEx.initCause(ex);
            throw ioEx;
        }
    }

}
