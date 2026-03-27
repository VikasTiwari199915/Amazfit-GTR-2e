package com.vikas.gtr2e.beans;

import java.util.Objects;

import lombok.Data;
import lombok.NoArgsConstructor;
/*
 * Parser for Huami device music information
 * Based on the reference implementation from Gadgetbridge
 * Licensed under AGPLv3
 * Some changes might have been made to better fit the needs of this project.
 *
 * Modifications by Vikas Tiwari
 */

@NoArgsConstructor
@Data
public class MusicBean {
    public static final int MUSIC_UNKNOWN = -1;
    public static final int MUSIC_UNDEFINED = 0;
    public static final int MUSIC_PLAY = 1;
    public static final int MUSIC_PAUSE = 2;
    public static final int MUSIC_PLAYPAUSE = 3;
    public static final int MUSIC_NEXT = 4;
    public static final int MUSIC_PREVIOUS = 5;

    public String artist = null;
    public String album = null;
    public String track = null;
    public int duration = MUSIC_UNKNOWN;
    public int trackCount = MUSIC_UNKNOWN;
    public int trackNr = MUSIC_UNKNOWN;

    public MusicBean(MusicBean old) {
        this.duration = old.duration;
        this.trackCount = old.trackCount;
        this.trackNr = old.trackNr;
        this.track = old.track;
        this.album = old.album;
        this.artist = old.artist;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof MusicBean musicBean)) {
            return false;
        }
        return Objects.equals(this.artist, musicBean.artist) &&
                Objects.equals(this.album, musicBean.album) &&
                Objects.equals(this.track, musicBean.track) &&
                this.duration == musicBean.duration &&
                this.trackCount == musicBean.trackCount &&
                this.trackNr == musicBean.trackNr;
    }

    @Override
    public int hashCode() {
        int result = artist != null ? artist.hashCode() : 0;
        result = 31 * result + (album != null ? album.hashCode() : 0);
        result = 31 * result + (track != null ? track.hashCode() : 0);
        result = 31 * result + duration;
        result = 31 * result + trackCount;
        result = 31 * result + trackNr;
        return result;
    }
}
