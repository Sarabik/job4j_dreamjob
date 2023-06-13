package ru.job4j.dreamjob.repository;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.job4j.dreamjob.configuration.DatasourceConfiguration;
import ru.job4j.dreamjob.model.File;
import ru.job4j.dreamjob.model.Candidate;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static java.time.LocalDateTime.now;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class Sql2oCandidateRepositoryTest {

    private static Sql2oCandidateRepository sql2oCandidateRepository;

    private static Sql2oFileRepository sql2oFileRepository;

    private static File file;

    @BeforeAll
    public static void initRepositories() throws Exception {
        var properties = new Properties();
        try (var inputStream = Sql2oCandidateRepositoryTest.class.getClassLoader().getResourceAsStream("connection.properties")) {
            properties.load(inputStream);
        }
        var url = properties.getProperty("datasource.url");
        var username = properties.getProperty("datasource.username");
        var password = properties.getProperty("datasource.password");

        var configuration = new DatasourceConfiguration();
        var datasource = configuration.connectionPool(url, username, password);
        var sql2o = configuration.databaseClient(datasource);

        sql2oCandidateRepository = new Sql2oCandidateRepository(sql2o);
        sql2oFileRepository = new Sql2oFileRepository(sql2o);

        file = new File("test", "test");
        sql2oFileRepository.save(file);
    }

    @AfterAll
    public static void deleteFile() {
        sql2oFileRepository.deleteById(file.getId());
    }

    @AfterEach
    public void clearCandidates() {
        var candidates = sql2oCandidateRepository.findAll();
        for (var candidate : candidates) {
            sql2oCandidateRepository.deleteById(candidate.getId());
        }
    }

    @Test
    public void whenSaveThenGetSame() {
        LocalDateTime creationDate = now().truncatedTo(ChronoUnit.MINUTES);
        Candidate candidate = sql2oCandidateRepository.save(new Candidate(0, "name", "description", creationDate, true, 1, file.getId()));
        Candidate savedCandidate = sql2oCandidateRepository.findById(candidate.getId()).get();
        assertThat(savedCandidate).usingRecursiveComparison().isEqualTo(candidate);
    }

    @Test
    public void whenSaveSeveralThenGetAll() {
        LocalDateTime creationDate = now().truncatedTo(ChronoUnit.MINUTES);
        Candidate candidate1 = sql2oCandidateRepository.save(new Candidate(0, "name1", "description1", creationDate, true, 1, file.getId()));
        Candidate candidate2 = sql2oCandidateRepository.save(new Candidate(0, "name2", "description2", creationDate, false, 1, file.getId()));
        Candidate candidate3 = sql2oCandidateRepository.save(new Candidate(0, "name3", "description3", creationDate, true, 1, file.getId()));
        Collection<Candidate> result = sql2oCandidateRepository.findAll();
        assertThat(result).isEqualTo(List.of(candidate1, candidate2, candidate3));
    }

    @Test
    public void whenDontSaveThenNothingFound() {
        assertThat(sql2oCandidateRepository.findAll()).isEqualTo(emptyList());
        assertThat(sql2oCandidateRepository.findById(0)).isEqualTo(empty());
    }

    @Test
    public void whenDeleteThenGetEmptyOptional() {
        LocalDateTime creationDate = now().truncatedTo(ChronoUnit.MINUTES);
        Candidate candidate = sql2oCandidateRepository.save(new Candidate(0, "name", "description", creationDate, true, 1, file.getId()));
        boolean isDeleted = sql2oCandidateRepository.deleteById(candidate.getId());
        Optional<Candidate> savedCandidate = sql2oCandidateRepository.findById(candidate.getId());
        assertThat(isDeleted).isTrue();
        assertThat(savedCandidate).isEqualTo(empty());
    }

    @Test
    public void whenDeleteByInvalidIdThenGetFalse() {
        assertThat(sql2oCandidateRepository.deleteById(0)).isFalse();
    }

    @Test
    public void whenUpdateThenGetUpdated() {
        LocalDateTime creationDate = now().truncatedTo(ChronoUnit.MINUTES);
        Candidate candidate = sql2oCandidateRepository.save(new Candidate(0, "name", "description", creationDate, true, 1, file.getId()));
        Candidate updatedCandidate = new Candidate(
                candidate.getId(), "new title", "new description", creationDate.plusDays(1),
                !candidate.getVisible(), 1, file.getId()
        );
        boolean isUpdated = sql2oCandidateRepository.update(updatedCandidate);
        Candidate savedCandidate = sql2oCandidateRepository.findById(updatedCandidate.getId()).get();
        assertThat(isUpdated).isTrue();
        assertThat(savedCandidate).usingRecursiveComparison().isEqualTo(updatedCandidate);
    }

    @Test
    public void whenUpdateUnExistingCandidateThenGetFalse() {
        LocalDateTime creationDate = now().truncatedTo(ChronoUnit.MINUTES);
        Candidate candidate = new Candidate(0, "name", "description", creationDate, true, 1, file.getId());
        boolean isUpdated = sql2oCandidateRepository.update(candidate);
        assertThat(isUpdated).isFalse();
    }
}