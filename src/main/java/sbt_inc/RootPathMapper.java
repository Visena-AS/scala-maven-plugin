/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package sbt_inc;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import sbt.internal.inc.CompileOutput;
import xsbti.VirtualFileRef;
import xsbti.compile.FileHash;
import xsbti.compile.MiniOptions;
import xsbti.compile.MiniSetup;
import xsbti.compile.Output;
import xsbti.compile.analysis.ReadMapper;
import xsbti.compile.analysis.Stamp;
import xsbti.compile.analysis.WriteMapper;

/**
 * Replaces named roots with ${NAME} in the parts of the analysis that the file converter does not
 * cover: the output directory, classpath entries and compiler options (e.g. -Xplugin:jar paths).
 * File references and stamps are left alone; the converter and content hashes take care of them.
 */
final class RootPathMapper implements ReadMapper, WriteMapper {

  private final List<Map.Entry<String, String>> tokenToRoot = new ArrayList<>();
  private final boolean writing;

  RootPathMapper(Map<String, Path> roots, boolean writing) {
    this.writing = writing;
    for (Map.Entry<String, Path> root : roots.entrySet()) {
      tokenToRoot.add(
          new AbstractMap.SimpleImmutableEntry<>(
              "${" + root.getKey() + "}", root.getValue().toString()));
    }
    // the longest root first, so a root nested inside another is replaced before its parent
    tokenToRoot.sort((a, b) -> b.getValue().length() - a.getValue().length());
  }

  private String map(String value) {
    String mapped = value;
    for (Map.Entry<String, String> root : tokenToRoot) {
      mapped =
          writing
              ? mapped.replace(root.getValue(), root.getKey())
              : mapped.replace(root.getKey(), root.getValue());
    }
    return mapped;
  }

  private Path map(Path path) {
    return Paths.get(map(path.toString()));
  }

  @Override
  public VirtualFileRef mapSourceFile(VirtualFileRef sourceFile) {
    return sourceFile;
  }

  @Override
  public VirtualFileRef mapBinaryFile(VirtualFileRef binaryFile) {
    return binaryFile;
  }

  @Override
  public VirtualFileRef mapProductFile(VirtualFileRef productFile) {
    return productFile;
  }

  @Override
  public Path mapOutputDir(Path outputDir) {
    return map(outputDir);
  }

  @Override
  public Path mapSourceDir(Path sourceDir) {
    return map(sourceDir);
  }

  @Override
  public Path mapClasspathEntry(Path classpathEntry) {
    return map(classpathEntry);
  }

  @Override
  public String mapJavacOption(String javacOption) {
    return map(javacOption);
  }

  @Override
  public String mapScalacOption(String scalacOption) {
    return map(scalacOption);
  }

  @Override
  public Stamp mapBinaryStamp(VirtualFileRef file, Stamp binaryStamp) {
    return binaryStamp;
  }

  @Override
  public Stamp mapSourceStamp(VirtualFileRef file, Stamp sourceStamp) {
    return sourceStamp;
  }

  @Override
  public Stamp mapProductStamp(VirtualFileRef file, Stamp productStamp) {
    return productStamp;
  }

  @Override
  public MiniSetup mapMiniSetup(MiniSetup setup) {
    Output output = setup.output();
    Optional<Path> singleOutput = output.getSingleOutputAsPath();
    Output mappedOutput =
        singleOutput.isPresent() ? CompileOutput.apply(map(singleOutput.get())) : output;
    MiniOptions options = setup.options();
    FileHash[] classpath =
        Arrays.stream(options.classpathHash())
            .map(entry -> FileHash.of(map(entry.file()), entry.hash()))
            .toArray(FileHash[]::new);
    String[] scalacOptions =
        Arrays.stream(options.scalacOptions()).map(this::map).toArray(String[]::new);
    String[] javacOptions =
        Arrays.stream(options.javacOptions()).map(this::map).toArray(String[]::new);
    return setup
        .withOutput(mappedOutput)
        .withOptions(MiniOptions.of(classpath, scalacOptions, javacOptions));
  }
}
