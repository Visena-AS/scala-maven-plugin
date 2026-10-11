/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package sbt_inc;

import java.nio.file.Path;
import java.util.*;
import sbt.internal.inc.MappedFileConverter;
import scala.jdk.javaapi.CollectionConverters;
import xsbti.FileConverter;
import xsbti.compile.analysis.ReadWriteMappers;

/**
 * How incremental analysis refers to files: absolute paths (the default), or paths relative to
 * named roots, so analysis written in one build tree stays valid in another.
 */
public final class AnalysisRelocation {

  public static final AnalysisRelocation NONE =
      new AnalysisRelocation(ReadWriteMappers.getEmptyMappers(), Optional.empty());

  private final ReadWriteMappers mappers;
  private final Optional<FileConverter> converter;

  private AnalysisRelocation(ReadWriteMappers mappers, Optional<FileConverter> converter) {
    this.mappers = mappers;
    this.converter = converter;
  }

  /** Roots by name, e.g. BASE for the reactor root; each is stored as ${NAME}. */
  public static AnalysisRelocation relativeTo(Map<String, Path> roots) {
    if (roots.isEmpty()) {
      return NONE;
    }
    scala.collection.immutable.Map<String, Path> scalaRoots =
        scala.collection.immutable.Map$.MODULE$.from(CollectionConverters.asScala(roots));
    return new AnalysisRelocation(
        new ReadWriteMappers(new RootPathMapper(roots, false), new RootPathMapper(roots, true)),
        Optional.of(new MappedFileConverter(scalaRoots, true)));
  }

  public ReadWriteMappers mappers() {
    return mappers;
  }

  public Optional<FileConverter> converter() {
    return converter;
  }
}
