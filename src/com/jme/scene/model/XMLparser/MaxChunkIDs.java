package com.jme.scene.model.XMLparser;

/**
 * Started Date: Jun 26, 2004<br><br>
 *
 * List of 3ds Chunk ID #'s
 * @author Jack Lindamood
 */
public interface MaxChunkIDs {

    // These must all be diffrent values
    static final int MAIN_3DS     =0x4D4D;
    static final int M3D_VERSION  =0x0002;
    static final int NULL_CHUNK   =0x0000;
    static final int MASTER_SCALE =0x0100;
    static final int NAMED_OBJECT =0x4000;
    static final int OBJ_TRIMESH  =0x4100;
    static final int VERTEX_LIST  =0x4110;
    static final int TEXT_COORDS  =0x4140;
    static final int COORD_SYS    =0x4160;
    static final int FACES_ARRAY  =0x4120;
    static final int SMOOTH_GROUP =0x4150;
    static final int KEYFRAMES    =0xb000;
    static final int KEYFRAME_HEAD=0xb00a;
    static final int XDATA_SECTION=0x8000;
    static final int ROT_TRACK_TAG=0xb021;
    static final int SCALE_TRACK  =0xb022;
    static final int EDIT_3DS     =0x3d3d;
    static final int MESH_VERSION =0x3d3e;
    static final int KEYFRAME_OBJ =0xb002;
    static final int NODE_ID      =0xb030;
    static final int TRACK_HEADER =0xb010;
    static final int TRACK_PIVOT  =0xb013;
    static final int TRACK_POS_TAG=0xb020;
    static final int TRACK_ROT_TAG=0xb021;
    static final int TRACK_SCL_TAG=0xb022;
    static final int KEY_SEGMENT  =0xb008;
    static final int KEY_CURTIME  =0xb009;
    static final int MAT_BLOCK    =0xafff;
    static final int MAT_NAME     =0xa000;
    static final int MAT_AMB_COLOR=0xa010;
    static final int COLOR_BYTE   =0x0011;
    static final int CLR_BYTE_GAMA=0x0012;
    static final int MAT_DIF_COLOR=0xa020;
    static final int MAT_SPEC_CLR =0xa030;
    static final int MAT_SHINE    =0xa040;
    static final int MAT_SHINE_STR=0xa041;
    static final int PRCT_INT_FRMT=0x0030;
    static final int PRCT_FLT_FRMT=0x0031;
    static final int MAT_ALPHA    =0xa050;
    static final int MAT_ALPHA_FAL=0xa052;
    static final int MAT_REF_BLUR =0xa053;
    static final int MAT_SHADING  =0xa100;
    static final int MAT_SELF_ILUM=0xa084;
    static final int MAT_WIRE_SIZE=0xa087;
    static final int VIEWPORT_LAYOUT=0x7001;
    static final int VIEWPORT_SIZE=0x7020;
    static final int VIEWPORT_DATA3=0x7012;
    static final int VIEWPORT_DATA=0x7011;
    static final int SHADOW_BIAS  =0x1400;
    static final int SHADOW_MAP_SIZE=0x1420;
    static final int SHADOW_MAP_RANGE=0x1450;
    static final int RAYTRACE_BIAS=0x1460;
    static final int O_CONSTS     =0x1500;
    static final int GEN_AMB_COLOR=0x2100;
    static final int COLOR_FLOAT  =0x0010;
    static final int CLR_FLOAT_GAMA=0x0013;
    static final int BACKGRD_COLOR=0x1200;
    static final int BACKGRD_BITMAP=0x1100;
    static final int V_GRADIENT    =0x1300;
    static final int USE_BCK_COLOR =0x1201;
    static final int FOG_FLAG      =0x2200;
    static final int FOG_BACKGROUND=0x2210;
    static final int LAYERED_FOG_OPT=0x2302;
    static final int DISTANCE_QUEUE=0x2300;
    static final int DQUEUE_BACKGRND=0x2310;
    static final int DEFAULT_VIEW   =0x3000;
    static final int VIEW_CAMERA    =0x3080;
    static final int VERTEX_OPTIONS =0x4111;
    static final int MESH_COLOR     =0x4165;
    static final int MESH_MAT_GROUP =0x4130;
    static final int CAMERA_FLAG    =0x4700;
    static final int CAMERA_RANGES  =0x4720;
    static final int LIGHT_OBJ      =0x4600;
    static final int LIGHT_OUT_RANGE=0x465a;
    static final int LIGHT_IN_RANGE =0x4659;
    static final int LIGHT_MULTIPLIER=0x465b;
    static final int MORPH_SMOOTH   =0xb015;
    static final int CAMERA_TARG_INF_TAG=0xb004;
    static final int CAMERA_INFO_TAG=0xb003;
    static final int KEY_FOV_TRACK  =0xb023;
    static final int KEY_ROLL_TRACK =0xb024;
    static final int KEY_OMNI_LI_INFO=0xb005;
    static final int KEY_COLOR_TRACK=0xb025;
    static final int KEY_AMBIENT_NODE=0xb001;
}
