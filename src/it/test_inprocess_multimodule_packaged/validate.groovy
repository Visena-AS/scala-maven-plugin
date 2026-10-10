try {
  assert new File(basedir, 'mod-a/target/mod-a-1.0-SNAPSHOT.jar').exists() : "mod-a was not packaged"
  assert new File(basedir, 'mod-a/target/mod-a-1.0-SNAPSHOT-tests.jar').exists() : "mod-a test-jar was not packaged"
  assert new File(basedir, 'mod-b/target/classes/b/B.class').exists() : "mod-b did not compile"
  assert new File(basedir, 'mod-b/target/test-classes/b/BCheck.class').exists() : "mod-b tests did not compile"

  // The real assertion: the second build finds the analysis behind mod-a's repacked jar and
  // test-jar, so zinc does not treat them as changed libraries and recompile mod-b.
  def log = new File(basedir, "build.log").text
  def compilations = log.readLines().findAll { it =~ /compiling \d+ Scala sources? to / }
  assert compilations.size() == 4 : "expected only the first build to compile (4 compilations), got:\n" + compilations.join("\n")

  return true
} catch (Throwable e) {
  e.printStackTrace()
  return false
}
