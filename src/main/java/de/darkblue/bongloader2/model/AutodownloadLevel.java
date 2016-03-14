/*
 * Copyright (C) 2016 Florian Frankenberger.
 *
 * This library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, see <http://www.gnu.org/licenses/>.
 */
package de.darkblue.bongloader2.model;

import de.darkblue.bongloader2.model.Recording.MovieFile.Quality;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Autodownload levels (as not all recordings have an HD version, we can't just
 * have a permutation of quality levels)
 *
 * @author Florian Frankenberger
 */
public enum AutodownloadLevel {
    HD_OR_HQ_AND_NQ("in High Definition (HD) oder in High Quality und in Normal Quality",
            new OR(Quality.HD, Quality.HQ), new AND(Quality.NQ)),

    HD_OR_HQ("nur in High Definition (HD) oder in High Quality",
            new OR(Quality.HD, Quality.HQ)),

    HQ("nur in High Quality",
            new AND(Quality.HQ)),

    HQ_AND_NQ("in High Quality und in Normal Quality",
            new AND(Quality.HQ, Quality.NQ)),

    NQ("nur in Normal Quality",
            new AND(Quality.NQ)),

    NONE("kein automatischer download")
    ;

    private static interface AutodownloadResolver {
        /**
         * should return a set of qualties to download
         * @param recording
         * @return
         */
        Set<Quality> resolve(Recording recording);
    }

    private static class OR implements AutodownloadResolver {
        private final Quality qualityA, qualityB;

        /**
         * first item is preferred if both are present
         *
         * @param qualityA
         * @param qualityB
         */
        OR(Quality qualityA, Quality qualityB) {
            this.qualityA = qualityA;
            this.qualityB = qualityB;
        }

        @Override
        public Set<Quality> resolve(Recording recording) {
            if (recording.hasMovieFile(qualityA)) {
                return EnumSet.of(qualityA);
            } else
                if (recording.hasMovieFile(qualityB)) {
                    return EnumSet.of(qualityB);
                }
            return Collections.emptySet();
        }

    }

    private static class AND implements AutodownloadResolver {
        private final Quality[] qualities;

        /**
         * @param qualityA
         * @param qualityB
         */
        AND(Quality... qualities) {
            this.qualities = qualities;
        }

        @Override
        public Set<Quality> resolve(Recording recording) {
            final Set<Quality> result = EnumSet.noneOf(Quality.class);
            for (Quality quality : qualities) {
                if (recording.hasMovieFile(quality)) {
                    result.add(quality);
                }
            }
            return result;
        }

    }

    private final String humanReadable;
    private final AutodownloadResolver[] resolvers;

    private AutodownloadLevel(String humanReadable, AutodownloadResolver... resolvers) {
        this.humanReadable = humanReadable;
        this.resolvers = resolvers;
    }

    public String getHumanReadable() {
        return humanReadable;
    }

    /**
     * determines all qualities to download according to this autodownload level
     * and the given recording.
     *
     * @param recording
     * @return a set of qualities to download
     */
    public Set<Quality> getQualitiesToDownload(Recording recording) {
        Set<Quality> qualitiesToDownload = EnumSet.noneOf(Quality.class);

        for (AutodownloadResolver resolver : resolvers) {
            qualitiesToDownload.addAll(resolver.resolve(recording));
        }

        return qualitiesToDownload;
    }

    public static AutodownloadLevel parse(String string) {
        for (AutodownloadLevel level : values()) {
            if (level.name().equalsIgnoreCase(string)) {
                return level;
            }
        }
        return NONE; //default
    }

    @Override
    public String toString() {
        return getHumanReadable();
    }

    /**
     * converts the old format of qualities to the new AutodownloadLevel
     * @param qualities
     */
    public static AutodownloadLevel convert(Set<Quality> qualities) {
        if (qualities.contains(Quality.HQ) && qualities.contains(Quality.NQ)) {
            return AutodownloadLevel.HQ_AND_NQ;
        } else
            if (qualities.contains(Quality.HQ)) {
                return AutodownloadLevel.HQ;
            } else
                if (qualities.contains(Quality.NQ)) {
                    return AutodownloadLevel.NQ;
                }

        return NONE;
    }

}
