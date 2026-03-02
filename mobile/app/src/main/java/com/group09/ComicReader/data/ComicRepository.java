package com.group09.ComicReader.data;

import com.group09.ComicReader.model.Chapter;
import com.group09.ComicReader.model.Comic;
import com.group09.ComicReader.model.CommentItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ComicRepository {

    private static ComicRepository instance;
    private final List<Comic> comics;
    private final List<Chapter> chapters;
    private final List<CommentItem> comments;

    private ComicRepository() {
        comics = buildComics();
        chapters = buildChapters();
        comments = buildComments();
    }

    public static ComicRepository getInstance() {
        if (instance == null) {
            instance = new ComicRepository();
        }
        return instance;
    }

    public List<Comic> getComics() {
        return new ArrayList<>(comics);
    }

    public List<Comic> getTrendingComics() {
        List<Comic> result = new ArrayList<>();
        for (Comic comic : comics) {
            if (comic.isTrending()) {
                result.add(comic);
            }
        }
        return result;
    }

    public List<Comic> getDailyUpdates() {
        int end = Math.min(5, comics.size());
        return new ArrayList<>(comics.subList(0, end));
    }

    public List<Comic> getRecommended() {
        int end = Math.min(6, comics.size());
        return new ArrayList<>(comics.subList(0, end));
    }

    public List<Comic> getLibraryComics() {
        List<Comic> result = new ArrayList<>();
        for (Comic comic : comics) {
            if (comic.getProgress() != null) {
                result.add(comic);
            }
        }
        return result;
    }

    public Comic getComicById(int comicId) {
        for (Comic comic : comics) {
            if (comic.getId() == comicId) {
                return comic;
            }
        }
        return null;
    }

    public List<String> getFilters() {
        return Arrays.asList("All", "Action", "Romance", "Fantasy", "Sci-Fi", "Mystery");
    }

    public List<Comic> searchComics(String query, String filter) {
        String safeQuery = query == null ? "" : query.trim().toLowerCase(Locale.US);
        String safeFilter = filter == null ? "All" : filter;
        List<Comic> result = new ArrayList<>();
        for (Comic comic : comics) {
            boolean matchesQuery = safeQuery.isEmpty() || comic.getTitle().toLowerCase(Locale.US).contains(safeQuery);
            boolean matchesFilter = "All".equals(safeFilter) || comic.getGenres().contains(safeFilter);
            if (matchesQuery && matchesFilter) {
                result.add(comic);
            }
        }
        return result;
    }

    public List<Chapter> getChaptersForComic(int comicId) {
        return new ArrayList<>(chapters);
    }

    public List<CommentItem> getCommentsForComic(int comicId) {
        return new ArrayList<>(comments);
    }

    private List<Comic> buildComics() {
        List<Comic> list = new ArrayList<>();
        list.add(new Comic(1, "Shadow Realm Chronicles", "Jin Park",
                "https://images.unsplash.com/photo-1769874825261-ef30d63f6817?auto=format&fit=crop&w=1080&q=80",
                4.8, Arrays.asList("Fantasy", "Action", "Adventure"),
                "A warrior masters shadow power to save his kingdom.", 100, 15, true, false));
        list.add(new Comic(2, "The Last Elementalist", "Sarah Chen",
                "https://images.unsplash.com/photo-1768159904119-297a25a08c8e?auto=format&fit=crop&w=1080&q=80",
                4.9, Arrays.asList("Fantasy", "Magic"),
                "The last elementalist restores balance across four realms.", 85, 42, true, false));
        list.add(new Comic(3, "Cyber Ronin 2099", "Takeshi Yamamoto",
                "https://images.unsplash.com/photo-1762895158802-507fb6d7aa7e?auto=format&fit=crop&w=1080&q=80",
                4.7, Arrays.asList("Sci-Fi", "Action", "Cyberpunk"),
                "A cyber samurai uncovers a city-wide conspiracy.", 120, 67, false, true));
        list.add(new Comic(4, "Hearts in Bloom", "Min-ji Kim",
                "https://images.unsplash.com/photo-1675552561547-fc440a0025c9?auto=format&fit=crop&w=1080&q=80",
                4.6, Arrays.asList("Romance", "Drama"),
                "Rival florists discover love in a competitive district.", 95, 28, false, false));
        list.add(new Comic(5, "Crimson Awakening", "Alex Rivera",
                "https://images.unsplash.com/photo-1763315371311-f59468cc2ddc?auto=format&fit=crop&w=1080&q=80",
                4.9, Arrays.asList("Action", "Mystery"),
                "A detective seeing spirits investigates impossible crimes.", 78, null, true, false));
        list.add(new Comic(6, "The Silent Witness", "Emma Brooks",
                "https://images.unsplash.com/photo-1648960094467-c7b7e0d7705d?auto=format&fit=crop&w=1080&q=80",
                4.5, Arrays.asList("Thriller", "Mystery"),
                "A forensic artist enters a killer's game.", 110, null, false, false));
        list.add(new Comic(7, "Midnight Enigma", "Lucas Fontaine",
                "https://images.unsplash.com/photo-1760578360191-0908de251220?auto=format&fit=crop&w=1080&q=80",
                4.4, Arrays.asList("Mystery", "Horror"),
                "A midnight door appears and people vanish.", 65, null, false, true));
        list.add(new Comic(8, "Starbound Academy", "Nova Sterling",
                "https://images.unsplash.com/photo-1697588501368-039d39ef3880?auto=format&fit=crop&w=1080&q=80",
                4.7, Arrays.asList("Sci-Fi", "Adventure"),
                "Elite students compete in an interstellar academy.", 92, null, false, false));
        return list;
    }

    private List<Chapter> buildChapters() {
        List<Chapter> list = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            boolean premium = i >= 15;
            String title = "Chapter " + i;
            String releaseDate = "Mar " + (i < 10 ? "0" + i : i) + ", 2026";
            list.add(new Chapter(i, i, title, premium, releaseDate));
        }
        return list;
    }

    private List<CommentItem> buildComments() {
        return new ArrayList<>(Arrays.asList(
                new CommentItem(1, "MangaFan2024", "", "This chapter was incredible!", "2 hours ago", 234),
                new CommentItem(2, "ComicReader88", "", "The art keeps getting better.", "5 hours ago", 187),
                new CommentItem(3, "WebtoonAddict", "", "Need the next chapter now.", "1 day ago", 342)
        ));
    }
}
