package com.g3d.audio;

/**
 * Interface to be implemented by audio renderers.
 * @author Kirill
 */
public interface AudioRenderer {

    /**
     * @param listener The listener camera, all 3D sounds will be
     * oriented around the listener.
     */
    public void setListener(Listener listener);

    /**
     * Sets the environment, used for reverb effects.
     *
     * @see PointAudioSource#setReverbEnabled(boolean)
     * @param env The environment to set.
     */
    public void setEnvironment(Environment env);

    public void playSourceInstance(AudioSource src);
    public void playSource(AudioSource src);
    public void pauseSource(AudioSource src);
    public void stopSource(AudioSource src);

    public void deleteAudioData(AudioData ad);

    /**
     * Initializes the renderer. Should be the first method called
     * before using the system.
     */
    public void initialize();

    /**
     * Update the audio system. Must be called periodically.
     * @param tpf Time per frame.
     */
    public void update(float tpf);

    /**
     * Cleanup/destroy the audio system. Call this when app closes.
     */
    public void cleanup();
}