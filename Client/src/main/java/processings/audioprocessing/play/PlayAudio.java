package processings.audioprocessing.play;
import javax.sound.sampled.*;

public class PlayAudio implements AutoCloseable{
    //класс воспроизводящий записанное аудио
    private class Listener implements LineListener {
        public void update(LineEvent ev) {
            if (ev.getType() == LineEvent.Type.STOP) {
                playing = false;
                synchronized(clip) {
                    clip.notify();
                    if(doAfterPlaying != null)doAfterPlaying.run();
                }
            }
        }
    }
    private boolean released = false;
    private Clip clip = null;
    private boolean playing = false;
    private final PlayAudio.Listener lineListener;
    private final Runnable doAfterPlaying;

    public PlayAudio(byte[] audioData, AudioFormat audioFormat, Runnable doAfterPlaying){
        lineListener = new PlayAudio.Listener();
        this.doAfterPlaying = doAfterPlaying;
        init(audioData, audioFormat);
    }
    public long getMicrosecondLength(){
        return clip.getMicrosecondLength();
    }
    public boolean init(byte[] audioData, AudioFormat audioFormat){
        if(released)return true;
        try{
            clip = AudioSystem.getClip();
            clip.open(audioFormat, audioData, 0, audioData.length);
            clip.addLineListener(lineListener);
            released = true;
        }
        catch (LineUnavailableException e){
            e.printStackTrace();
            close();
        }
        return released;
    }
    // true если звук успешно загружен, false если произошла ошибка
    public boolean isReleased() {
        return released;
    }

    // проигрывается ли звук в данный момент
    public boolean isPlaying() {
        return playing;
    }

    // Запуск
	/*
	  breakOld определяет поведение, если звук уже играется
	  Если breakOld==true, о звук будет прерван и запущен заново
	  Иначе ничего не произойдёт
	*/
    public void play(boolean breakOld) {
        if (released) {
            if (breakOld) {
                clip.stop();
                clip.setFramePosition(0);
                clip.start();
                playing = true;
            } else if (!isPlaying()) {
                clip.setFramePosition(0);
                clip.start();
                playing = true;
            }
        }
    }

    // То же самое, что и play(true)
    public void play() {
        play(true);
    }

    // Останавливает воспроизведение
    public void stop() {
        if (playing) {
            clip.stop();
        }
    }

    public void close() {
        if (clip != null)
            clip.close();
    }
}
