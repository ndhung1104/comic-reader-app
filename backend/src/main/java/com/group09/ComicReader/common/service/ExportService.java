package com.group09.ComicReader.common.service;

import com.group09.ComicReader.auth.entity.UserEntity;
import com.group09.ComicReader.auth.repository.UserRepository;
import com.group09.ComicReader.comic.entity.ComicEntity;
import com.group09.ComicReader.comic.repository.ComicRepository;
import com.group09.ComicReader.wallet.entity.WalletTransactionEntity;
import com.group09.ComicReader.wallet.repository.WalletTransactionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ExportService {

    private final WalletTransactionRepository walletTransactionRepository;
    private final ComicRepository comicRepository;
    private final UserRepository userRepository;

    public ExportService(WalletTransactionRepository walletTransactionRepository,
                         ComicRepository comicRepository,
                         UserRepository userRepository) {
        this.walletTransactionRepository = walletTransactionRepository;
        this.comicRepository = comicRepository;
        this.userRepository = userRepository;
    }

    public String exportRevenueCsv(LocalDateTime from, LocalDateTime to) {
        System.out.println("Generating Revenue CSV from " + from + " to " + to);
        List<WalletTransactionEntity> transactions = walletTransactionRepository.findAllByDateRange(from, to);
        System.out.println("Found " + transactions.size() + " transactions");
        StringBuilder sb = new StringBuilder();
        sb.append("ID,UserID,Amount,Type,ReferenceID,CreatedAt\n");
        for (WalletTransactionEntity t : transactions) {
            sb.append(t.getId()).append(",")
              .append(t.getUser().getId()).append(",")
              .append(t.getAmount()).append(",")
              .append(t.getType()).append(",")
              .append(escapeCsv(t.getReferenceId())).append(",")
              .append(t.getCreatedAt()).append("\n");
        }
        return sb.toString();
    }

    public String exportContentCsv(LocalDateTime from, LocalDateTime to) {
        System.out.println("Generating Content CSV from " + from + " to " + to);
        List<ComicEntity> comics = comicRepository.findAllByDateRange(from, to);
        System.out.println("Found " + comics.size() + " comics");
        StringBuilder sb = new StringBuilder();
        sb.append("ID,Title,Author,Slug,ViewCount,FollowerCount,Rating,CreatedAt\n");
        for (ComicEntity c : comics) {
            sb.append(c.getId()).append(",")
              .append(escapeCsv(c.getTitle())).append(",")
              .append(escapeCsv(c.getAuthor())).append(",")
              .append(escapeCsv(c.getSlug())).append(",")
              .append(c.getViewCount()).append(",")
              .append(c.getFollowerCount()).append(",")
              .append(c.getAverageRating()).append(",")
              .append(c.getCreatedAt()).append("\n");
        }
        return sb.toString();
    }

    public String exportUserActivityCsv(LocalDateTime from, LocalDateTime to) {
        System.out.println("Generating User CSV from " + from + " to " + to);
        List<UserEntity> users = userRepository.findAllByDateRange(from, to);
        System.out.println("Found " + users.size() + " users");
        StringBuilder sb = new StringBuilder();
        sb.append("ID,FullName,Email,Role,Enabled,CreatedAt\n");
        for (UserEntity u : users) {
            sb.append(u.getId()).append(",")
              .append(escapeCsv(u.getFullName())).append(",")
              .append(escapeCsv(u.getEmail())).append(",")
              .append(escapeCsv(u.getRoles().toString())).append(",")
              .append(u.isEnabled()).append(",")
              .append(u.getCreatedAt()).append("\n");
        }
        return sb.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
