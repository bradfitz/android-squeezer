package uk.org.ngo.squeezer.test.model;

import java.util.HashMap;

import uk.org.ngo.squeezer.model.SqueezerAlbum;
import uk.org.ngo.squeezer.model.SqueezerSong;
import android.test.AndroidTestCase;

public class SqueezerSongTest extends AndroidTestCase {
    SqueezerSong song1, song2, song3;
    HashMap<String, String> record1, record2, record3;

    /**
     * Verify that the equals() method compares correctly against nulls, other
     * item types, and is reflexive (a = a), symmetric (a = b && b = a), and
     * transitive (a = b && b = c && a = c).
     */
    public void testEquals() {
        record1 = new HashMap<String, String>();
        record2 = new HashMap<String, String>();
        record3 = new HashMap<String, String>();

        song1 = new SqueezerSong(record1);
        song2 = new SqueezerSong(record2);
        song3 = new SqueezerSong(record3);

        assertTrue("A song equals itself (reflexive)", song1.equals(song1));

        assertFalse("A song, even an empty one, is not equal to null",
                song1.equals(null));

        assertTrue("Two songs with null IDs are equal", song1.equals(song2));
        assertTrue("... and is symmetric", song2.equals(song1));

        SqueezerAlbum album1 = new SqueezerAlbum(record1);
        assertFalse("Null song does not equal a null album", song1.equals(album1));
        assertFalse("... and is symmetric", album1.equals(song1));

        song1.setId("1");
        song2.setId("2");
        assertFalse("Songs with different IDs are different", song1.equals(song2));
        assertFalse("... and is symmetric", song2.equals(song1));

        song1.setId("1");
        song2.setId("1");
        song3.setId("1");
        assertTrue("Songs with the same ID are equivalent", song1.equals(song2));
        assertTrue("... and is symmetric", song2.equals(song1));
        assertTrue("... and is transitive (1)", song2.equals(song3));
        assertTrue("... and is transitive (2)", song1.equals(song3));

        song1.setId("1");
        song1.setName("Song");
        song2.setId("2");
        song2.setName("Song");
        assertFalse("Songs with same name but different IDs are different", song1.equals(song2));
        assertFalse("... and is symmetric", song2.equals(song1));

        song1.setId("1");
        album1.setId("1");
        assertFalse("Songs and albums with the same ID are different", song1.equals(album1));
        assertFalse("... and is symmetric", album1.equals(song1));
    }
}
