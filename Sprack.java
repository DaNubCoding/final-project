import greenfoot.*;

/**
 * A sprite stack (Sprack) is a sprite made up of multiple layers of 2D images,
 * each rotated to the same angle, but with a varying vertical offset. This
 * creates the illusion of a 3D object.
 * <p>
 * Note that the Spracks are not actually dynamically rendered layer by layer,
 * but are instead pre-rendered and stored in a {@link SprackView} object.
 * <p>
 * Every Sprack has a looping animation. This animation plays and loops
 * continuously but may be overridden by a one-time animation. A one-time
 * animation plays until completion once and then returns to the looping
 * animation. A looping animation or one-time animation may be replaced
 * immediately at any time by using the {@link #setLoopingAnimation()} and
 * {@link #playOneTimeAnimation()} methods.
 *
 * @author Martin Baldwin
 * @version June 2024
 *
 * @see SprackView
 */
public class Sprack extends Sprite implements WorldObject {
    // The current looping animation of this Sprack
    private Animation loopingAnimation;
    // The current one-time animation of this Sprack, or null if none
    private Animation oneTimeAnimation;

    private Vector3 worldPos;
    private double rotation;

    /**
     * Create a new Sprack with the given fixed sheet name.
     * <p>
     * The sheet name is used to look up the {@link SprackView} object that
     * contains the pre-rendered images for this Sprack.
     *
     * @param sheetName the name of the Sprack sheet
     */
    public Sprack(String sheetName) {
        this(sheetName, Layer.SPRACK_DEFAULT);
    }

    /**
     * Create a new Sprack with the given fixed sheet name and layer.
     * <p>
     * The sheet name is used to look up the {@link SprackView} object that
     * contains the pre-rendered images for this Sprack.
     *
     * @param sheetName the name of the Sprack sheet
     * @param layer the layer to render the Sprack on
     */
    public Sprack(String sheetName, Layer layer) {
        this(new Animation(-1, sheetName), layer);
    }

    /**
     * Create a new Sprack with the given looping animation of sheet names.
     * <p>
     * The sheet names are used to look up the {@link SprackView} objects that
     * contain the pre-rendered images for the animation.
     *
     * @param sheetAnimation an {@link Animation} object describing the looping animation of SprackView names to assign to this Sprack
     */
    public Sprack(Animation sheetAnimation) {
        this(sheetAnimation, Layer.SPRACK_DEFAULT);
    }

    /**
     * Create a new Sprack with the given looping animation of sheet names and
     * layer.
     * <p>
     * The sheet names are used to look up the {@link SprackView} objects that
     * contain the pre-rendered images for the animation.
     *
     * @param sheetAnimation an {@link Animation} object describing the looping animation of SprackView names to assign to this Sprack
     * @param layer the layer to render the Sprack on
     */
    public Sprack(Animation sheetAnimation, Layer layer) {
        super(layer);
        loopingAnimation = sheetAnimation;
        oneTimeAnimation = null;
        worldPos = new Vector3();
    }

    /**
     * Set the in-world rotation of the Sprack.
     *
     * @param rotation the rotation of the Sprack, in degrees
     */
    @Override
    public void setWorldRotation(double rotation) {
        this.rotation = Vector2.normalizeAngle(rotation);
    }

    /**
     * Get the in-world rotation of the Sprack.
     * <p>
     * The rotation is in degrees, and is normalized to the range [0, 360).
     *
     * @return the rotation of the Sprack, in degrees
     */
    @Override
    public double getWorldRotation() {
        return rotation;
    }

    /**
     * Set the world position of the Sprack.
     * <p>
     * The world position is the position of the Sprack in the game world, not
     * the screen position.
     *
     * @param x the x position
     * @param y the y position
     * @param z the z position
     */
    @Override
    public void setWorldPos(double x, double y, double z) {
        worldPos = new Vector3(x, y, z);
    }

    /**
     * Set the world position of the Sprack.
     * <p>
     * The world position is the position of the Sprack in the game world, not
     * the screen position.
     *
     * @param position the position
     */
    @Override
    public void setWorldPos(Vector3 position) {
        worldPos = position;
    }

    /**
     * Set the current looping animation of this Sprack.
     * <p>
     * If the given animation is not the current looping animation, it will be
     * reset to its beginning (via {@link Animation#reset()}).
     *
     * @param animation the looping animation to replace this Sprack's current looping animation
     */
    public void setLoopingAnimation(Animation animation) {
        if (animation != loopingAnimation) {
            animation.reset();
        }
        loopingAnimation = animation;
    }

    /**
     * Play an animation one time on this Sprack, overriding its looping
     * animation and previous one-time animation, if any. The given animation
     * will be reset to its beginning (via {@link Animation#reset()}) and it
     * will continue to be displayed until it reaches its end, at which point
     * this Sprack will return to its current looping animation.
     *
     * @param animation an animation to play to completion once on this Sprack
     */
    public void playOneTimeAnimation(Animation animation) {
        animation.reset();
        oneTimeAnimation = animation;
    }

    /**
     * Return the current looping animation of this Sprack.
     *
     * @return the current looping animation
     */
    public Animation getCurrentLoopingAnimation() {
        return loopingAnimation;
    }

    /**
     * Return the current one-time animation of this Sprack, if any.
     *
     * @return the current one-time animation, or null if none
     */
    public Animation getCurrentOneTimeAnimation() {
        return oneTimeAnimation;
    }

    @Override
    public void render(GreenfootImage canvas) {
        loopingAnimation.update();
        Animation currentAnimation = loopingAnimation;
        if (oneTimeAnimation != null) {
            oneTimeAnimation.update();
            // Return to the looping animation once the one-time animation is over
            if (oneTimeAnimation.hasLooped()) {
                oneTimeAnimation = null;
            } else {
                currentAnimation = oneTimeAnimation;
            }
        }

        SprackView view = SprackView.getView(currentAnimation.getCurrentName());
        if (view == null) {
            return;
        }

        // Update screen position, rotated around zoomed camera position
        double scale = Camera.getZoom();
        double offsetX = (worldPos.x - Camera.getX()) * scale;
        double offsetY = (worldPos.y - Camera.getY()) * scale;
        double offsetZ = (worldPos.z - Camera.getZ()) * scale;
        double screenRad = Math.toRadians(-Camera.getRotation());
        double screenX = getWorld().getWidth() / 2 + offsetX * Math.cos(screenRad) - offsetZ * Math.sin(screenRad);
        double screenY = getWorld().getHeight() / 2 + offsetX * Math.sin(screenRad) + offsetZ * Math.cos(screenRad);
        screenY -= offsetY;
        setScreenPos(screenX, screenY);

        // Don't render if offscreen
        double imageRotation = rotation - Camera.getRotation();
        int centerX = view.getCenterX(imageRotation, scale);
        int centerY = view.getCenterY(imageRotation, scale);
        if (screenX + centerX < 0
            || screenX - centerX >= getWorld().getWidth()
            || screenY + (view.getTransformedHeight(imageRotation, scale) - centerY) < 0
            || screenY - centerY >= getWorld().getHeight()) {
            return;
        }

        // Draw image, screen position at center of bottom layer
        GreenfootImage image = view.getTransformedImage(imageRotation, Camera.getZoom());
        if (image == null) {
            return;
        }
        canvas.drawImage(image, (int) screenX - centerX, (int) screenY - centerY);
    }

    /**
     * Get the x position of the Sprack in the world.
     *
     * @return the x position of the Sprack
     */
    @Override
    public double getWorldX() {
        return worldPos.x;
    }

    /**
     * Get the y position of the Sprack in the world.
     *
     * @return the y position of the Sprack
     */
    @Override
    public double getWorldY() {
        return worldPos.y;
    }

    /**
     * Get the z position of the Sprack in the world.
     *
     * @return the z position of the Sprack
     */
    @Override
    public double getWorldZ() {
        return worldPos.z;
    }

    /**
     * Get the world position of the Sprack.
     *
     * @return the world position of the Sprack
     */
    @Override
    public Vector3 getWorldPos() {
        return worldPos;
    }

    /**
     * Get the rotation of the Sprack as it appears on the screen, relative to
     * the camera rotation.
     *
     * @return the visual rotation of the Sprack
     */
    public double getVisualRotation() {
        return Vector2.normalizeAngle(rotation - Camera.getRotation());
    }

    /**
     * Get the world the Sprack is in, this is always an instance of a
     * {@link SpriteStackingWorld} for Spracks.
     *
     * @return the world the Sprack is in
     */
    @Override
    public SpriteStackingWorld getWorld() {
        return (SpriteStackingWorld) super.getWorld();
    }
}
