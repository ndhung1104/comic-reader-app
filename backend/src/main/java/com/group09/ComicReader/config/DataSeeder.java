package com.group09.ComicReader.config;

import com.group09.ComicReader.auth.entity.RoleEntity;
import com.group09.ComicReader.auth.entity.UserEntity;
import com.group09.ComicReader.auth.repository.RoleRepository;
import com.group09.ComicReader.auth.repository.UserRepository;
import com.group09.ComicReader.chapter.entity.ChapterEntity;
import com.group09.ComicReader.chapter.entity.ChapterPageEntity;
import com.group09.ComicReader.chapter.repository.ChapterPageRepository;
import com.group09.ComicReader.chapter.repository.ChapterRepository;
import com.group09.ComicReader.comic.entity.ComicEntity;
import com.group09.ComicReader.comic.repository.ComicRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ComicRepository comicRepository;
    private final ChapterRepository chapterRepository;
    private final ChapterPageRepository chapterPageRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(RoleRepository roleRepository,
                      UserRepository userRepository,
                      ComicRepository comicRepository,
                      ChapterRepository chapterRepository,
                      ChapterPageRepository chapterPageRepository,
                      PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.comicRepository = comicRepository;
        this.chapterRepository = chapterRepository;
        this.chapterPageRepository = chapterPageRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        RoleEntity adminRole = roleRepository.findByName("ADMIN").orElseGet(() -> createRole("ADMIN"));
        RoleEntity userRole = roleRepository.findByName("USER").orElseGet(() -> createRole("USER"));

        if (!userRepository.existsByEmail("admin@comicreader.dev")) {
            UserEntity admin = new UserEntity();
            admin.setEmail("admin@comicreader.dev");
            admin.setFullName("Admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRoles(Set.of(adminRole));
            userRepository.save(admin);
        }

        if (!userRepository.existsByEmail("user@comicreader.dev")) {
            UserEntity user = new UserEntity();
            user.setEmail("user@comicreader.dev");
            user.setFullName("Demo User");
            user.setPassword(passwordEncoder.encode("user123"));
            user.setRoles(Set.of(userRole));
            userRepository.save(user);
        }

        if (comicRepository.count() == 0) {
            ComicEntity comic = new ComicEntity();
            comic.setTitle("Starter Comic");
            comic.setAuthor("System Seeder");
            comic.setStatus("PUBLISHED");
            comic.setSynopsis("Seeded comic for development testing");
            comic.setCoverUrl("https://images.unsplash.com/photo-1697588501368-039d39ef3880?auto=format&fit=crop&w=1080&q=80");
            ComicEntity savedComic = comicRepository.save(comic);

            ChapterEntity chapter = new ChapterEntity();
            chapter.setComic(savedComic);
            chapter.setChapterNumber(1);
            chapter.setTitle("Chapter 1");
            chapter.setPremium(false);
            ChapterEntity savedChapter = chapterRepository.save(chapter);

            List<String> seededPages = List.of(
                    "https://images.unsplash.com/photo-1760578360191-0908de251220?auto=format&fit=crop&w=1080&q=80",
                    "https://images.unsplash.com/photo-1763315371311-f59468cc2ddc?auto=format&fit=crop&w=1080&q=80"
            );

            for (int i = 0; i < seededPages.size(); i++) {
                ChapterPageEntity page = new ChapterPageEntity();
                page.setChapter(savedChapter);
                page.setPageNumber(i + 1);
                page.setImageUrl(seededPages.get(i));
                chapterPageRepository.save(page);
            }
        }
    }

    private RoleEntity createRole(String roleName) {
        RoleEntity role = new RoleEntity();
        role.setName(roleName);
        return roleRepository.save(role);
    }
}

