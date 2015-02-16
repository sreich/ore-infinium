package com.ore.infinium;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
 
public class BetterSprite {
 
    protected TextureRegion textureRegion;
 
    protected boolean visible = true;
    protected Vector2 unscaledPivotPoint;
 
    protected Vector2 bottomLeftCornerOfTexture;
    protected int regionWidth;
    protected int regionHeight;
 
    protected Rectangle boundingBox;
 
    protected Vector2 scale = new Vector2(1.0f, 1.0f);
    protected Vector2 position = new Vector2();
    protected float rotation = 0;
    protected Vector2 pivotPointRatio = new Vector2(0.5f, 0.5f);
 
    protected Color color = new Color(Color.WHITE);
 
    public BetterSprite(TextureRegion textureRegion) {
 
        this.textureRegion = textureRegion;
        regionWidth = textureRegion.getRegionWidth();
        regionHeight = textureRegion.getRegionHeight();
 
        unscaledPivotPoint = new Vector2();
        bottomLeftCornerOfTexture = new Vector2();
        boundingBox = new Rectangle(0, 0, regionWidth, regionHeight);
 
        setPivotPointRatio(0.5f, 0.5f);
    }
 
    public float getAlpha() {
        return color.a;
    }
 
    /**
     * @return The height of the texture region in pixels.
     */
    public int getHeight() {
        return textureRegion.getRegionHeight();
    }
 
    /**
     * @return The scaled height of the texture region in pixels;
     */
    public float getScaledHeight() {
        return textureRegion.getRegionHeight() * scale.y;
    }
 
    /**
     * @return The scaled width of the texture region in pixels.
     */
    public float getScaledWidth() {
        return textureRegion.getRegionWidth() * scale.x;
    }
 
    public Texture getTexture() {
        return textureRegion.getTexture();
    }
 
    /**
     * @return The width of the texture region in pixels.
     */
    public int getWidth() {
        return textureRegion.getRegionWidth();
    }
 
    public boolean isVisible() {
        return visible;
    }
 
    public void render(SpriteBatch batch) {
 
        if(visible == false || color.a == 0) return;
 
        batch.setColor(color);
 
        batch.draw(
                textureRegion,
                bottomLeftCornerOfTexture.x, bottomLeftCornerOfTexture.y,
                pivotPointRatio.x * regionWidth,
                pivotPointRatio.y * regionHeight,
                regionWidth, regionHeight,
                scale.x, scale.y,
                rotation
        );
    }
 
    public void setAlpha(float alpha) {
        color.a = alpha;
    }
 
    public void setHeight(float height) {
        scale.y = height / regionHeight;
        updateBoundingBox();
    }
 
    public void setPivotPointRatio(float x, float y) {
        pivotPointRatio.set(x, y);
 
        bottomLeftCornerOfTexture.set(
                position.x - x * textureRegion.getRegionWidth(),
                position.y - y * textureRegion.getRegionHeight()
        );
 
        unscaledPivotPoint.set(
                bottomLeftCornerOfTexture.x + x * textureRegion.getRegionWidth(),
                bottomLeftCornerOfTexture.y + y * textureRegion.getRegionHeight()
        );
 
        updateBoundingBox();
    }
     
    public void setPosition(Vector2 position) {
        setPosition(position.x, position.y);
    }
 
    public void setPosition(float x, float y) {
 
        float dx = x - position.x;
        float dy = y - position.y;
 
        bottomLeftCornerOfTexture.x += dx;
        bottomLeftCornerOfTexture.y += dy;
        unscaledPivotPoint.x += dx;
        unscaledPivotPoint.y += dy;
 
        boundingBox.x += dx;
        boundingBox.y += dy;
 
        position.set(x, y);
    }
 
    public void setScale(float scale) {
        this.scale.set(scale, scale);
        updateBoundingBox();
    }
 
    public void setScaleX(float scaleX) {
        scale.x = scaleX;
        updateBoundingBox();
    }
 
    public void setScaleY(float scaleY) {
        scale.y = scaleY;
        updateBoundingBox();
    }
 
    public void setSize(float width, float height) {
        scale.x = width / regionWidth;
        scale.y = height / regionHeight;
        updateBoundingBox();
    }
 
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
 
    public void setWidth(float width) {
        scale.x = width / regionWidth;
        updateBoundingBox();
    }
 
    private void updateBoundingBox() {
 
        final float scaleX = regionWidth * scale.x;
        final float scaleY = regionHeight * scale.y;
 
        boundingBox.x = position.x - pivotPointRatio.x * scaleX;
        boundingBox.y = position.y - pivotPointRatio.y * scaleY;
        boundingBox.width = scaleX;
        boundingBox.height = scaleY;
    }
 
    public float getRotation() {
        return rotation;
    }
 
    public void setRotation(float rotation) {
        this.rotation = rotation;
    }
 
    public Color getColor() {
        return color;
    }
 
    public void setColor(Color color) {
        this.color = color;
    }
 
    public void setColor(float r, float g, float b, float a) {
        color.set(r, g, b, a);
    }
     
    public Vector2 getPosition() {
        return position;
    }
     
    public Vector2 getScale() {
        return scale;
    }
     
    public float getScaleX() {
        return scale.x;
    }
     
    public float getScaleY() {
        return scale.y;
    }
}