package uk.org.ngo.squeezer.test.framework;

import android.test.ActivityInstrumentationTestCase2;

import java.util.Arrays;
import java.util.List;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.ItemAdapter;
import uk.org.ngo.squeezer.itemlist.ArtistListActivity;
import uk.org.ngo.squeezer.itemlist.ArtistView;
import uk.org.ngo.squeezer.model.Artist;

public class ItemAdapterTest extends ActivityInstrumentationTestCase2<ArtistListActivity> {

    private ItemAdapter<Artist> artistItemAdapter;
    private Artist[] artists;
    int pageSize;

    public ItemAdapterTest() {
        super(null, ArtistListActivity.class);
        artists = getArtists();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        artistItemAdapter = new ItemAdapter<Artist>(new ArtistView(getActivity()));
        pageSize = getInstrumentation().getTargetContext().getResources().getInteger(R.integer.PageSize);

        // Test that the adapter is initially empty
        assertEquals(0, artistItemAdapter.getCount());
    }

    @Override
    protected void tearDown() throws Exception {
        artistItemAdapter.clear();
        super.tearDown();
    }

    public void testUpdate() {
        // Test adding all items at once
        artistItemAdapter.update(artists.length, 0, Arrays.asList(artists));
        assertEquals(artists.length, artistItemAdapter.getCount());
        for (int i = 0; i < artists.length; i++) {
            assertEquals(artists[i], artistItemAdapter.getItem(i));
        }

        // Add the items in pages roughly as we we do it when loading pages sequentially
        addFirstPage();
        for (int pos = pageSize; pos < artists.length; pos += pageSize) {
            int end = pos + pageSize <= artists.length ? pos + pageSize : artists.length;
            artistItemAdapter.update(artists.length, pos, Arrays.asList(artists).subList(pos, end));
            for (int i = 0; i < artistItemAdapter.getCount(); i++) {
                assertEquals(i < end ? artists[i] : null, artistItemAdapter.getItem(i));
            }
        }

        List<Integer> selectedPages = Arrays.asList(5, 2, 3, 4, 1);

        // Add the items in pages roughly as we we do when the scrolls the list randomly
        addFirstPage();
        for (int p = 0; p < selectedPages.size(); p++) {
            int pos = selectedPages.get(p) * pageSize;
            int end = pos+pageSize <= artists.length ? pos + pageSize : artists.length;
            artistItemAdapter.update(artists.length, pos, Arrays.asList(artists).subList(pos, end));
            for (int i = 0; i < artistItemAdapter.getCount(); i++) {
                int page = i / pageSize;
                boolean pageAdded = (page == 0) || selectedPages.subList(0, p+1).contains(page);
                assertEquals(pageAdded ? artists[i] : null, artistItemAdapter.getItem(i));
            }
        }

    }

    private void addFirstPage() {
        artistItemAdapter.clear();
        artistItemAdapter.update(artists.length, 0, Arrays.asList(artists[0]));
        assertEquals(artists.length, artistItemAdapter.getCount());
        for (int i = 0; i < artistItemAdapter.getCount(); i++) {
            assertEquals(i == 0 ? artists[i] : null, artistItemAdapter.getItem(i));
        }

        artistItemAdapter.update(artists.length, 1, Arrays.asList(artists).subList(1, pageSize));
        assertEquals(artists.length, artistItemAdapter.getCount());
        for (int i = 0; i < artistItemAdapter.getCount(); i++) {
            assertEquals(i < pageSize ? artists[i] : null, artistItemAdapter.getItem(i));
        }
    }

    public void testRemoveItem() {
        // For all items remove the first item.
        // For each item removed assert the resulting list is correct.
        artistItemAdapter.update(artists.length, 0, Arrays.asList(artists));
        for (int i = 1; i <= artists.length; i++) {
            artistItemAdapter.removeItem(0);
            assertEquals(artists.length - i, artistItemAdapter.getCount());
            for (int j = 0; j < artistItemAdapter.getCount(); j++) {
                assertEquals(artists[i+j], artistItemAdapter.getItem(j));
            }
        }

        // For all items remove the last item.
        // For each item removed assert the resulting list is correct.
        artistItemAdapter.update(artists.length, 0, Arrays.asList(artists));
        for (int i = 1; i <= artists.length; i++) {
            artistItemAdapter.removeItem(artists.length - i);
            assertEquals(artists.length - i, artistItemAdapter.getCount());
            for (int j = 0; j < artistItemAdapter.getCount(); j++) {
                assertEquals(artists[j], artistItemAdapter.getItem(j));
            }
        }
    }

    public void testInsertItem() {
        // Continuously add the first item, and test the resulting list
        artistItemAdapter.clear();
        for (int i = artists.length-1; i >= 0; i--) {
            artistItemAdapter.insertItem(0, artists[i]);
            assertEquals(artists.length - i, artistItemAdapter.getCount());
            for (int j = 0; j < artistItemAdapter.getCount(); j++) {
                assertEquals(artists[i+j], artistItemAdapter.getItem(j));
            }
        }

        // Continuously add the last item, and test the resulting list
        artistItemAdapter.clear();
        for (int i = 0; i < artists.length; i++) {
            artistItemAdapter.insertItem(i, artists[i]);
            assertEquals(i+1, artistItemAdapter.getCount());
            for (int j = 0; j < artistItemAdapter.getCount(); j++) {
                assertEquals(artists[j], artistItemAdapter.getItem(j));
            }
        }
    }

    public void testSelectedItems() {
        List<Integer> selectedItems = Arrays.asList(19, 20, 22, 2, 1, 0, 105, 106, 107, 108, 100,
                101, 102, 103, 104, 3, 4, 5, 6, 7, 8, 9, 10, 11, 70, 72, 75, 80, 71, 79, 73, 54, 52,
                50, 60, 59, 57, 55);

        // Check findItem
        artistItemAdapter.update(artists.length, 0, Arrays.asList(artists));
        for (Integer selectedItem : selectedItems) {
            int pos = artistItemAdapter.findItem(artists[selectedItem]);
            assertEquals(selectedItem.intValue(), pos);
        }

        // Remove selected item, testing the resulting list
        for (int i = 0; i < selectedItems.size(); i++) {
            int pos = artistItemAdapter.findItem(artists[selectedItems.get(i)]);
            artistItemAdapter.removeItem(pos);
            assertEquals(artists.length - i - 1, artistItemAdapter.getCount());
            int skipCount = 0;
            for (int j = 0; j < artistItemAdapter.getCount(); j++) {
                List<Integer> subList = selectedItems.subList(0, i + 1);
                while (subList.contains(Integer.valueOf(artists[j + skipCount].getId())))
                    skipCount++;
                assertEquals(artists[j + skipCount], artistItemAdapter.getItem(j));
            }
        }

        // Insert selected item, testing the resulting list
        for (int i = 0; i < selectedItems.size(); i++) {
            int pos = 0;
            while (pos < artistItemAdapter.getCount() &&
                    Integer.valueOf(artistItemAdapter.getItem(pos).getId()) < selectedItems.get(i))
                pos++;
            artistItemAdapter.insertItem(pos, artists[selectedItems.get(i)]);
            assertEquals(artists.length - selectedItems.size() + i + 1, artistItemAdapter.getCount());
            int skipCount = 0;
            for (int j = 0; j < artistItemAdapter.getCount(); j++) {
                List<Integer> subList = selectedItems.subList(i+1, selectedItems.size());
                while (subList.contains(Integer.valueOf(artists[j + skipCount].getId())))
                    skipCount++;
                assertEquals(artists[j + skipCount], artistItemAdapter.getItem(j));
            }
        }
    }

    private Artist[] getArtists() {
        int N = 109;
        Artist[] result = new Artist[N];

        for (int i = 0; i < N; i++) {
            result[i] = new Artist(String.valueOf(i), "Artist " + i);
        }

        return result;
    }
}
