package com.group09.ComicReader.creatorrequest.service;

import com.group09.ComicReader.auth.entity.RoleEntity;
import com.group09.ComicReader.auth.entity.UserEntity;
import com.group09.ComicReader.auth.repository.RoleRepository;
import com.group09.ComicReader.auth.repository.UserRepository;
import com.group09.ComicReader.common.exception.BadRequestException;
import com.group09.ComicReader.common.exception.NotFoundException;
import com.group09.ComicReader.creatorrequest.entity.CreatorRequestEntity;
import com.group09.ComicReader.creatorrequest.entity.CreatorRequestStatus;
import com.group09.ComicReader.creatorrequest.repository.CreatorRequestRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class CreatorRequestService {

    private final CreatorRequestRepository repository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public CreatorRequestService(CreatorRequestRepository repository, UserRepository userRepository,
            RoleRepository roleRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional
    public CreatorRequestEntity createRequest(UserEntity user, String message) {
        if (repository.existsByUserAndStatus(user, CreatorRequestStatus.PENDING)) {
            throw new BadRequestException("You already have a pending creator request");
        }

        CreatorRequestEntity req = new CreatorRequestEntity();
        req.setUser(user);
        req.setMessage(message);
        req.setStatus(CreatorRequestStatus.PENDING);
        return repository.save(req);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<CreatorRequestEntity> getLatestRequestByUser(UserEntity user) {
        return repository.findTopByUserOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public Page<CreatorRequestEntity> listRequests(Pageable pageable) {
        return repository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public CreatorRequestEntity getRequest(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Creator request not found"));
    }

    @Transactional
    public CreatorRequestEntity approveRequest(Long requestId, UserEntity admin, String adminMessage) {
        CreatorRequestEntity req = getRequest(requestId);
        if (req.getStatus() != CreatorRequestStatus.PENDING) {
            throw new BadRequestException("Request already processed");
        }

        UserEntity target = req.getUser();

        RoleEntity creatorRole = roleRepository.findByName("CREATOR").orElseGet(() -> {
            RoleEntity r = new RoleEntity();
            r.setName("CREATOR");
            return roleRepository.save(r);
        });

        target.getRoles().add(creatorRole);
        userRepository.save(target);

        req.setStatus(CreatorRequestStatus.APPROVED);
        req.setProcessedAt(LocalDateTime.now());
        req.setProcessedBy(admin);
        req.setAdminMessage(adminMessage);
        return repository.save(req);
    }

    @Transactional
    public CreatorRequestEntity denyRequest(Long requestId, UserEntity admin, String adminMessage) {
        CreatorRequestEntity req = getRequest(requestId);
        if (req.getStatus() != CreatorRequestStatus.PENDING) {
            throw new BadRequestException("Request already processed");
        }

        req.setStatus(CreatorRequestStatus.DENIED);
        req.setProcessedAt(LocalDateTime.now());
        req.setProcessedBy(admin);
        req.setAdminMessage(adminMessage);
        return repository.save(req);
    }
}
