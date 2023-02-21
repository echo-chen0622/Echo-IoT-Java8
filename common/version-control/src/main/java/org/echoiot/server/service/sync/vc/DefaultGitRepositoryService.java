package org.echoiot.server.service.sync.vc;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.EntityIdFactory;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.sync.vc.*;
import org.echoiot.server.service.sync.vc.GitRepository.Diff;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@ConditionalOnProperty(prefix = "vc", value = "git.service", havingValue = "local", matchIfMissing = true)
@Service
public class DefaultGitRepositoryService implements GitRepositoryService {

    @Value("${java.io.tmpdir}/repositories")
    private String defaultFolder;

    @Value("${vc.git.repositories-folder:${java.io.tmpdir}/repositories}")
    private String repositoriesFolder;

    private final Map<TenantId, GitRepository> repositories = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        if (StringUtils.isEmpty(repositoriesFolder)) {
            repositoriesFolder = defaultFolder;
        }
    }

    @NotNull
    @Override
    public Set<TenantId> getActiveRepositoryTenants() {
        return new HashSet<>(repositories.keySet());
    }

    @Override
    public void prepareCommit(@NotNull PendingCommit commit) {
        GitRepository repository = checkRepository(commit.getTenantId());
        String branch = commit.getBranch();
        try {
            repository.fetch();

            repository.createAndCheckoutOrphanBranch(commit.getWorkingBranch());
            repository.resetAndClean();

            if (repository.listRemoteBranches().contains(new BranchInfo(branch, false))) {
                repository.merge(branch);
            }
        } catch (IOException | GitAPIException gitAPIException) {
            //TODO: analyze and return meaningful exceptions that we can show to the client;
            throw new RuntimeException(gitAPIException);
        }
    }

    @Override
    public void deleteFolderContent(@NotNull PendingCommit commit, String relativePath) throws IOException {
        GitRepository repository = checkRepository(commit.getTenantId());
        FileUtils.deleteDirectory(Path.of(repository.getDirectory(), relativePath).toFile());
    }

    @Override
    public void add(@NotNull PendingCommit commit, String relativePath, String entityDataJson) throws IOException {
        GitRepository repository = checkRepository(commit.getTenantId());
        FileUtils.write(Path.of(repository.getDirectory(), relativePath).toFile(), entityDataJson, StandardCharsets.UTF_8);
    }

    @NotNull
    @Override
    public VersionCreationResult push(@NotNull PendingCommit commit) {
        GitRepository repository = checkRepository(commit.getTenantId());
        try {
            repository.add(".");

            @NotNull VersionCreationResult result = new VersionCreationResult();
            @NotNull GitRepository.Status status = repository.status();
            result.setAdded(status.getAdded().size());
            result.setModified(status.getModified().size());
            result.setRemoved(status.getRemoved().size());

            if (result.getAdded() > 0 || result.getModified() > 0 || result.getRemoved() > 0) {
                @NotNull GitRepository.Commit gitCommit = repository.commit(commit.getVersionName(), commit.getAuthorName(), commit.getAuthorEmail());
                repository.push(commit.getWorkingBranch(), commit.getBranch());
                result.setVersion(toVersion(gitCommit));
            }
            return result;
        } catch (GitAPIException gitAPIException) {
            //TODO: analyze and return meaningful exceptions that we can show to the client;
            throw new RuntimeException(gitAPIException);
        } finally {
            cleanUp(commit);
        }
    }

    @SneakyThrows
    @Override
    public void cleanUp(@NotNull PendingCommit commit) {
        log.debug("[{}] Cleanup tenant repository started.", commit.getTenantId());
        GitRepository repository = checkRepository(commit.getTenantId());
        try {
            repository.createAndCheckoutOrphanBranch(EntityId.NULL_UUID.toString());
        } catch (Exception e) {
            if (!e.getMessage().contains("NO_CHANGE")) {
                throw e;
            }
        }
        repository.resetAndClean();
        repository.deleteLocalBranchIfExists(commit.getWorkingBranch());
        log.debug("[{}] Cleanup tenant repository completed.", commit.getTenantId());
    }

    @Override
    public void abort(@NotNull PendingCommit commit) {
        cleanUp(commit);
    }

    @Override
    public void fetch(TenantId tenantId) throws GitAPIException {
        var repository = repositories.get(tenantId);
        if (repository != null) {
            log.debug("[{}] Fetching tenant repository.", tenantId);
            repository.fetch();
            log.debug("[{}] Fetched tenant repository.", tenantId);
        }
    }

    @Override
    public String getFileContentAtCommit(TenantId tenantId, @NotNull String relativePath, String versionId) throws IOException {
        GitRepository repository = checkRepository(tenantId);
        return repository.getFileContentAtCommit(relativePath, versionId);
    }

    @Override
    public List<Diff> getVersionsDiffList(TenantId tenantId, @NotNull String path, String versionId1, String versionId2) throws IOException {
        GitRepository repository = checkRepository(tenantId);
        return repository.getDiffList(versionId1, versionId2, path);
    }

    @Override
    public String getContentsDiff(TenantId tenantId, @NotNull String content1, @NotNull String content2) throws IOException {
        GitRepository repository = checkRepository(tenantId);
        return repository.getContentsDiff(content1, content2);
    }

    @Override
    public List<BranchInfo> listBranches(TenantId tenantId) {
        GitRepository repository = checkRepository(tenantId);
        try {
            return repository.listRemoteBranches();
        } catch (GitAPIException gitAPIException) {
            //TODO: analyze and return meaningful exceptions that we can show to the client;
            throw new RuntimeException(gitAPIException);
        }
    }

    private GitRepository checkRepository(TenantId tenantId) {
        return Optional.ofNullable(repositories.get(tenantId))
                .orElseThrow(() -> new IllegalStateException("Repository is not initialized"));
    }

    @Override
    public PageData<EntityVersion> listVersions(TenantId tenantId, String branch, String path, @NotNull PageLink pageLink) throws Exception {
        GitRepository repository = checkRepository(tenantId);
        return repository.listCommits(branch, path, pageLink).mapData(this::toVersion);
    }

    @NotNull
    @Override
    public List<VersionedEntityInfo> listEntitiesAtVersion(TenantId tenantId, String versionId, @NotNull String path) throws Exception {
        GitRepository repository = checkRepository(tenantId);
        return repository.listFilesAtCommit(versionId, path).stream()
                .map(filePath -> {
                    EntityId entityId = fromRelativePath(filePath);
                    @NotNull VersionedEntityInfo info = new VersionedEntityInfo();
                    info.setExternalId(entityId);
                    return info;
                })
                .collect(Collectors.toList());
    }

    @Override
    public void testRepository(TenantId tenantId, @NotNull RepositorySettings settings) throws Exception {
        @NotNull Path testDirectory = Path.of(repositoriesFolder, "repo-test-" + UUID.randomUUID());
        GitRepository.test(settings, testDirectory.toFile());
    }

    @Override
    public void initRepository(@NotNull TenantId tenantId, @NotNull RepositorySettings settings) throws Exception {
        testRepository(tenantId, settings);

        clearRepository(tenantId);
        log.debug("[{}] Init tenant repository started.", tenantId);
        @NotNull Path repositoryDirectory = Path.of(repositoriesFolder, tenantId.getId().toString());
        GitRepository repository;
        if (Files.exists(repositoryDirectory)) {
            FileUtils.forceDelete(repositoryDirectory.toFile());
        }

        Files.createDirectories(repositoryDirectory);
        repository = GitRepository.clone(settings, repositoryDirectory.toFile());
        repositories.put(tenantId, repository);
        log.debug("[{}] Init tenant repository completed.", tenantId);
    }

    @Nullable
    @Override
    public RepositorySettings getRepositorySettings(TenantId tenantId) throws Exception {
        var gitRepository = repositories.get(tenantId);
        return gitRepository != null ? gitRepository.getSettings() : null;
    }

    @Override
    public void clearRepository(TenantId tenantId) throws IOException {
        GitRepository repository = repositories.get(tenantId);
        if (repository != null) {
            log.debug("[{}] Clear tenant repository started.", tenantId);
            FileUtils.deleteDirectory(new File(repository.getDirectory()));
            repositories.remove(tenantId);
            log.debug("[{}] Clear tenant repository completed.", tenantId);
        }
    }

    @NotNull
    private EntityVersion toVersion(@NotNull GitRepository.Commit commit) {
        return new EntityVersion(commit.getTimestamp(), commit.getId(), commit.getMessage(), this.getAuthor(commit));
    }

    private String getAuthor(@NotNull GitRepository.Commit commit) {
        String author = String.format("<%s>", commit.getAuthorEmail());
        if (StringUtils.isNotBlank(commit.getAuthorName())) {
            author = String.format("%s %s", commit.getAuthorName(), author);
        }
        return author;
    }

    public static EntityId fromRelativePath(@NotNull String path) {
        @NotNull EntityType entityType = EntityType.valueOf(StringUtils.substringBefore(path, "/").toUpperCase());
        String entityId = StringUtils.substringBetween(path, "/", ".json");
        return EntityIdFactory.getByTypeAndUuid(entityType, entityId);
    }
}
