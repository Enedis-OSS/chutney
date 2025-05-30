/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.server.core.domain.scenario;

import static java.util.Optional.ofNullable;

import fr.enedis.chutney.server.core.domain.security.User;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

public final class TestCaseMetadataImpl implements TestCaseMetadata {

    public final String id;
    public final String title;
    public final String description;
    public final List<String> tags;
    public final Instant creationDate;
    public final String defaultDataset;
    public final Instant updateDate;
    public final String author;
    public final Integer version;

    private TestCaseMetadataImpl(String id, String title, String description, List<String> tags, Instant creationDate, String defaultDataset, Instant updateDate, String author, Integer version) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.tags = tags;
        this.creationDate = creationDate;
        this.defaultDataset = defaultDataset;
        this.updateDate = updateDate;
        this.author = author;
        this.version = version;
    }


    @Override
    public String id() {
        return id;
    }

    @Override
    public String defaultDataset() {
        return defaultDataset;
    }

    @Override
    public String title() {
        return title;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public Instant creationDate() {
        return creationDate;
    }

    @Override
    public List<String> tags() {
        return tags;
    }

    @Override
    public String author() {
        return author;
    }

    @Override
    public Instant updateDate() {
        return updateDate;
    }

    @Override
    public Integer version() {
        return version;
    }

    @Override
    public String toString() {
        return "TestCaseMetadata{" +
            "id=" + id +
            ", title='" + title + '\'' +
            ", description='" + description + '\'' +
            ", tags=" + tags +
            ", creationDate=" + creationDate +
            ", defaultDataset=" + defaultDataset +
            ", updateDate=" + updateDate +
            ", author=" + author +
            ", version=" + version +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TestCaseMetadataImpl that = (TestCaseMetadataImpl) o;
        return Objects.equals(id, that.id) &&
            Objects.equals(title, that.title) &&
            Objects.equals(description, that.description) &&
            Objects.equals(tags, that.tags) &&
            Objects.equals(creationDate, that.creationDate) &&
            Objects.equals(defaultDataset, that.defaultDataset) &&
            Objects.equals(updateDate, that.updateDate) &&
            Objects.equals(author, that.author) &&
            Objects.equals(version, that.version)
            ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, description, tags, creationDate, defaultDataset, updateDate, author, version);
    }

    public static TestCaseMetadataBuilder builder() {
        return new TestCaseMetadataBuilder();
    }

    public static class TestCaseMetadataBuilder {
        private String id;
        private String title;
        private String description;
        private List<String> tags;
        private Instant creationDate;
        private String defaultDataset;
        private Instant updateDate;
        private String author;
        private Integer version;

        private TestCaseMetadataBuilder() {
        }

        public TestCaseMetadataImpl build() {
            Instant creationDate = ofNullable(this.creationDate).orElse(Instant.now());
            return new TestCaseMetadataImpl(
                id,
                ofNullable(title).orElse(""),
                ofNullable(description).orElse(""),
                ofNullable(tags).stream().flatMap(Collection::stream).filter(StringUtils::isNotBlank).map(String::toUpperCase).map(String::trim).collect(Collectors.toList()),
                creationDate,
                ofNullable(defaultDataset).orElse(""),
                ofNullable(updateDate).orElse(creationDate),
                ofNullable(author).orElseGet(() -> User.ANONYMOUS.id),
                ofNullable(version).orElse(1));
        }

        public TestCaseMetadataBuilder withId(String id) {
            this.id = id;
            return this;
        }

        public TestCaseMetadataBuilder withDefaultDataset(String defaultDataset) {
            this.defaultDataset = defaultDataset;
            return this;
        }

        public TestCaseMetadataBuilder withTitle(String title) {
            this.title = title;
            return this;
        }

        public TestCaseMetadataBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public TestCaseMetadataBuilder withTags(List<String> tags) {
            this.tags = tags;
            return this;
        }

        public TestCaseMetadataBuilder withCreationDate(Instant creationDate) {
            this.creationDate = creationDate;
            return this;
        }

        public TestCaseMetadataBuilder withUpdateDate(Instant updateDate) {
            this.updateDate = updateDate;
            return this;
        }

        public TestCaseMetadataBuilder withAuthor(String author) {
            this.author = author;
            return this;
        }

        public TestCaseMetadataBuilder withVersion(Integer version) {
            this.version = version;
            return this;
        }

        public static TestCaseMetadataBuilder from(TestCaseMetadata testCaseMetadata) {
            return new TestCaseMetadataBuilder()
                .withId(testCaseMetadata.id())
                .withTitle(testCaseMetadata.title())
                .withDescription(testCaseMetadata.description())
                .withCreationDate(testCaseMetadata.creationDate())
                .withTags(testCaseMetadata.tags())
                .withDefaultDataset(testCaseMetadata.defaultDataset())
                .withUpdateDate(testCaseMetadata.updateDate())
                .withAuthor(testCaseMetadata.author())
                .withVersion(testCaseMetadata.version());
        }

    }

}
