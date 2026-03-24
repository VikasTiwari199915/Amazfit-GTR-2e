package com.vikas.gtr2e.beans;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MusicStateBean {
    public static final int STATE_UNKNOWN = -1;
    public static final int STATE_PLAYING = 0;
    public static final int STATE_PAUSED = 1;
    public static final int STATE_STOPPED = 2;
    public static final int STATE_SHUFFLE_ENABLED = 1;

    public byte state = STATE_UNKNOWN;
    /**
     * Position of the current media in seconds
     */
    public int position = STATE_UNKNOWN;
    /**
     * Speed of playback, usually 0 or 100 (full speed)
     */
    public int playRate = STATE_UNKNOWN;
    public byte shuffle = STATE_UNKNOWN;
    public byte repeat = STATE_UNKNOWN;


    public MusicStateBean(MusicStateBean old) {
        this.state = old.state;
        this.position = old.position;
        this.playRate = old.playRate;
        this.shuffle = old.shuffle;
        this.repeat = old.repeat;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof MusicStateBean stateSpec)) {
            return false;
        }

        return this.state == stateSpec.state &&
                Math.abs(this.position - stateSpec.position)<=2 &&
                this.playRate == stateSpec.playRate &&
                this.shuffle == stateSpec.shuffle &&
                this.repeat == stateSpec.repeat;
    }

    @Override
    public int hashCode() {
        int result = state;
        result = 31 * result + playRate;
        result = 31 * result + (int) shuffle;
        result = 31 * result + (int) repeat;
        return result;
    }

}
