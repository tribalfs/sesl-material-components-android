include(":lib")

rootProject.children.forEach { p ->
  val allChildren = mutableListOf<ProjectDescriptor>()
  var curChildren = p.children.toList()
  while (curChildren.isNotEmpty()) {
    allChildren.addAll(curChildren)
    curChildren = curChildren
      .mapNotNull { if (it.children.isEmpty()) null else it.children }
      .flatten()
  }

  allChildren.forEach { project ->
    // Give each project a repository-wide unique name based on their path from
    // the top-level dir (e.g. the project at java/io/material/catalog will
    // be named :java-io-material-catalog). Doing so avoids the need to
    // have unique directory names throughout our subprojects.
    project.name = project.path.substring(1).replace(':', '-')
  }
}
