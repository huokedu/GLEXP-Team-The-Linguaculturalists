package com.linguaculturalists.phoenicia;

import android.content.res.AssetManager;
import android.view.MotionEvent;

import org.andengine.engine.camera.Camera;
import org.andengine.engine.camera.ZoomCamera;
import org.andengine.engine.camera.hud.HUD;
import org.andengine.entity.scene.IOnSceneTouchListener;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.Background;
import org.andengine.entity.sprite.ButtonSprite;
import org.andengine.entity.sprite.Sprite;
import org.andengine.extension.tmx.TMXLayer;
import org.andengine.input.touch.TouchEvent;
import org.andengine.input.touch.detector.PinchZoomDetector;
import org.andengine.opengl.texture.TextureManager;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.TextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.bitmap.AssetBitmapTexture;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.texture.region.ITiledTextureRegion;
import org.andengine.opengl.texture.region.TextureRegionFactory;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.util.adt.color.Color;
import org.andengine.extension.tmx.TMXLoader;
import org.andengine.extension.tmx.TMXLoader.ITMXTilePropertiesListener;
import org.andengine.extension.tmx.TMXProperties;
import org.andengine.extension.tmx.TMXTile;
import org.andengine.extension.tmx.TMXTileProperty;
import org.andengine.extension.tmx.TMXTiledMap;
import org.andengine.extension.tmx.util.exception.TMXLoadException;
import org.andengine.util.debug.Debug;

import java.io.IOException;
import java.util.List;

/**
 * Created by mhall on 3/22/15.
 */
public class PhoeniciaGame {

    private TextureManager textureManager;
    private AssetManager assetManager;
    private VertexBufferObjectManager vboManager;
    public Scene scene;

    private float mPinchZoomStartedCameraZoomFactor;
    private PinchZoomDetector mPinchZoomDetector;

    public AssetBitmapTexture terrainTexture;
    public ITiledTextureRegion terrainTiles;

    public Sprite[][] placedSprites;

    private TMXTiledMap mTMXTiledMap;

    public int placeBlock = -1;

    public PhoeniciaGame(TextureManager textureManager, AssetManager assetManager, VertexBufferObjectManager vbo, final ZoomCamera camera) {
        this.textureManager = textureManager;
        this.assetManager = assetManager;
        this.vboManager = vbo;
        scene = new Scene();
        scene.setBackground(new Background(new Color(0, 0, 0)));

        mPinchZoomDetector = new PinchZoomDetector(new PinchZoomDetector.IPinchZoomDetectorListener() {
            @Override
            public void onPinchZoomStarted(final PinchZoomDetector pPinchZoomDetector, final TouchEvent pTouchEvent) {
                mPinchZoomStartedCameraZoomFactor = camera.getZoomFactor();
            }

            @Override
            public void onPinchZoom(final PinchZoomDetector pPinchZoomDetector, final TouchEvent pTouchEvent, final float pZoomFactor) {
                camera.setZoomFactor(mPinchZoomStartedCameraZoomFactor * pZoomFactor);
            }

            @Override
            public void onPinchZoomFinished(final PinchZoomDetector pPinchZoomDetector, final TouchEvent pTouchEvent, final float pZoomFactor) {
                camera.setZoomFactor(mPinchZoomStartedCameraZoomFactor * pZoomFactor);
            }
        });
        scene.setOnSceneTouchListener(new IOnSceneTouchListener() {
            @Override
            public boolean onSceneTouchEvent(final Scene pScene, final TouchEvent pSceneTouchEvent) {
                mPinchZoomDetector.onTouchEvent(pSceneTouchEvent);

                switch(pSceneTouchEvent.getAction()) {
                    case TouchEvent.ACTION_DOWN:
                        placeBlock((int) pSceneTouchEvent.getX(), (int) pSceneTouchEvent.getY());
                        break;
                    case TouchEvent.ACTION_UP:
                        //MainActivity.this.mSmoothCamera.setZoomFactor(1.0f);
                        break;
                    case TouchEvent.ACTION_MOVE:
                        MotionEvent motion = pSceneTouchEvent.getMotionEvent();
                        if(motion.getHistorySize() > 0){
                            for(int i = 1, n = motion.getHistorySize(); i < n; i++){
                                int calcX = (int) motion.getHistoricalX(i) - (int) motion.getHistoricalX(i-1);
                                int calcY = (int) motion.getHistoricalY(i) - (int) motion.getHistoricalY(i-1);
                                //System.out.println("diffX: "+calcX+", diffY: "+calcY);

                                camera.setCenter(camera.getCenterX() - calcX, camera.getCenterY() + calcY);
                            }
                        }
                        break;
                }
                return true;
            }
        });
    }

    public void load() throws IOException {
        terrainTexture = new AssetBitmapTexture(textureManager, assetManager, "textures/terrain.png");
        terrainTexture.load();
        terrainTiles = TextureRegionFactory.extractTiledFromTexture(terrainTexture, 0, 0, 640, 1024, 10, 16);

        try {
            final TMXLoader tmxLoader = new TMXLoader(assetManager, textureManager, TextureOptions.BILINEAR_PREMULTIPLYALPHA, vboManager);
            this.mTMXTiledMap = tmxLoader.loadFromAsset("textures/map.tmx");
            for (TMXLayer tmxLayer : this.mTMXTiledMap.getTMXLayers()){
                scene.attachChild(tmxLayer);
            }
            placedSprites = new Sprite[this.mTMXTiledMap.getTileColumns()][this.mTMXTiledMap.getTileRows()];
        } catch (final TMXLoadException e) {
            Debug.e(e);
        }
    }

    public void start(Camera camera) {
        TMXLayer baseLayer = this.mTMXTiledMap.getTMXLayers().get(0);
        camera.setCenter(400, -400);
        camera.setHUD(this.getBlockPlacementHUD());
    }
    public HUD getBlockPlacementHUD() {
        final HUD hud = new HUD();
        ITextureRegion blockRegion = terrainTiles.getTextureRegion(145);
        ButtonSprite block = new ButtonSprite(64, 48, blockRegion, vboManager);
        block.setOnClickListener(new ButtonSprite.OnClickListener() {
            @Override
            public void onClick(ButtonSprite buttonSprite, float v, float v2) {
                if (placeBlock == 145) {
                    placeBlock = -1;
                } else {
                    placeBlock = 145;
                }
            }
        });
        hud.registerTouchArea(block);
        hud.attachChild(block);

        ITextureRegion greyBlockRegion = terrainTiles.getTextureRegion(144);
        ButtonSprite greyBlock = new ButtonSprite(64*3, 48, greyBlockRegion, vboManager);
        greyBlock.setOnClickListener(new ButtonSprite.OnClickListener() {
            @Override
            public void onClick(ButtonSprite buttonSprite, float v, float v2) {
                if (placeBlock == 144) {
                    placeBlock = -1;
                } else {
                    placeBlock = 144;
                }
            }
        });
        hud.registerTouchArea(greyBlock);
        hud.attachChild(greyBlock);

        ITextureRegion bushBlockRegion = terrainTiles.getTextureRegion(126);
        ButtonSprite bushBlock = new ButtonSprite(64*5, 48, bushBlockRegion, vboManager);
        bushBlock.setOnClickListener(new ButtonSprite.OnClickListener() {
            @Override
            public void onClick(ButtonSprite buttonSprite, float v, float v2) {
                if (placeBlock == 126) {
                    placeBlock = -1;
                } else {
                    placeBlock = 126;
                }
            }
        });
        hud.registerTouchArea(bushBlock);
        hud.attachChild(bushBlock);
        return hud;
    }

    public void placeBlock(int x, int y) {
        if (placeBlock < 0) {
            System.out.println("No active block to place");
            return;
        }
        final TMXLayer tmxLayer = this.mTMXTiledMap.getTMXLayers().get(1);
        final TMXTile tmxTile = tmxLayer.getTMXTileAt(x, y);
        if (tmxTile == null) {
            System.out.println("Can't place blocks outside of map");
            return;
        }
        int tileX = (int)tmxTile.getTileX();// tiles are 64px wide, assume the touch is targeting the middle
        int tileY = (int)tmxTile.getTileY();// tiles are 64px wide, assume the touch is targeting the middle
        int tileZ = tmxTile.getTileZ();

        if (placedSprites[tmxTile.getTileColumn()][tmxTile.getTileRow()] != null) {
            scene.detachChild(placedSprites[tmxTile.getTileColumn()][tmxTile.getTileRow()]);
            placedSprites[tmxTile.getTileColumn()][tmxTile.getTileRow()] = null;
        }

        System.out.println("  px-x: "+x);
        System.out.println("  px-y: "+y);
        System.out.println("tile-x: "+tmxTile.getTileColumn());
        System.out.println("tile-y: "+tmxTile.getTileRow());
        System.out.println("tile-z: "+tileZ);

        ITextureRegion blockRegion = terrainTiles.getTextureRegion(placeBlock);
        Sprite block = new Sprite(tileX, tileY, blockRegion, vboManager);
        block.setZIndex(tileZ);
        placedSprites[tmxTile.getTileColumn()][tmxTile.getTileRow()] = block;
        scene.attachChild(block);

        scene.sortChildren();
    }

}
