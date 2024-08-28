# Contributing to Supporting LTE to 5G FR2 cell relationship creation.

This document describes how to contribute to the Supporting LTE to 5G FR2 cell relationship creation. It provides the information needed by the LTE nodes for ANR on the node to work with 5G FR2 cells Project.

## How to submit a feature request
Contact the PO mentioned in the [README.md]
Please provide a feature description and why this should be added to this service. Also, please describe the definition of done (DoD) criteria.

## Gerrit Project Details
Supporting LTE to 5G FR2 cell relationship creation. It provides the information needed by the LTE nodes for ANR on the node to work with 5G FR2 cells artifacts are stored in the Gerrit project: [OSS/com.ericsson.oss.apps/eric-oss-cnr5gassist](https://gerrit.ericsson.se/#/admin/projects/OSS/com.ericsson.oss.apps/eric-oss-cnr5gassist)

### Documents

The documentation for this service is located in the [doc folder](https://gerrit.ericsson.se/plugins/gitiles/OSS/com.ericsson.oss.apps/eric-oss-cnr5gassist/+/master/doc).

To update documents that are not in the referenced folder, contact the service guardians mentioned in the [README.md](https://gerrit.ericsson.se/plugins/gitiles/OSS/com.ericsson.oss.apps/eric-oss-cnr5gassist/+/master/README.md).

## Contact Information
Contact the PO mentioned in the [README.md]


## Contribution Workflow
1. The **contributor** updates the artifact in the local repository.
2. The **contributor** pushes the update to Gerrit for review, including a reference to the JIRA.
3. The **contributor** invites the **service guardians** (mandatory) and **other relevant parties** (optional) to the Gerrit review, and makes no further changes to the document until it is reviewed.
4. The **service guardians** review the document and give a code-review score.
   The code-review scores and corresponding workflow activities are as follows:
    - Score is +1
      A **reviewer** is happy with the changes but approval is required from another reviewer.
    - Score is +2
      The **service guardian** accepts the change and merges the code to master branch. The Publish pipeline is initiated to make the change available to consumers.
    - Score is -1 or -2
      The **contributor** follows-up on reviewer comments, until changes approved by service guardian.
    - The **service guardian** and the **contributor** align to determine when and how the change is published.

   [README.md](https://gerrit.ericsson.se/plugins/gitiles/OSS/com.ericsson.oss.apps/eric-oss-cnr5gassist/+/master/README.md)

---

## NOTE (To be removed)

This document is not a template.
You can use this document as an example of contributing guideline.
Please update/add/remove content from this example if you see needed.
If you have any suggestion to improve the example, please contact
PDLADPFRAM@pdl.internal.ericsson.com.

---

When contributing to this repository, please first discuss the change you wish
to make in the [discussion forum][forum] for the project or via
{% Project JIRA %}, email, or any other method with the
[guardians](#Project-Guardians) of this repository before making a change.

The following is a set of guidelines for contributing to {% Project Name %}
project. These are mostly guidelines, not rules. Use your best judgment, and
feel free to propose changes to this document submitting a patch.

## Table of Contents

[TOC]

## Code of Conduct

This project and everyone participating in it is governed by the
[ADP Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to
uphold this code.

## Project Guardians

The guardians are the maintainer of this project. They are responsible to
moderate the [discussion forum][forum], guide the contributors and review the
submitted patches.

- Stefano Volpe (<stefano.volpe@ericsson.com>)

## Development Environment prerequisites

The development framework for {% Project Name %} is based on [bob][bob]. To be
able to run bob, the following tools need to exist on the host:

- python3
- bash
- docker

Bob expects you to have a valid docker login towards your docker registry on the
host, currently it can't handle automatic login by itself. If you are using
armdocker, then you can login with the following command:

```text
docker login armdocker.rnd.ericsson.se
```

## How can I use this repository?

This repository contains the source code of {% Project Name %} service including
functional and test code, documentation and configuration files for manual and
automatic build and verification.

If you want to fix a bug or just want to experiment with adding a feature,
you'll want to try the service in your environment using a local copy of the
project's source.

You can start cloning the GIT repository to get your local copy:

```text
git clone ssh://<userid>@gerrit.ericsson.se:29418/<project-path>
```

Once you have your local copy, you shall navigate the root directory and
update the submodules needed to build the project:

```text
git submodule update --init --recursive
```

You can now build the service with the following command, from the root
directory:

```text
./bob/bob build image package
```

You can verify your build running the tests located in the folder `test`
using the following command:

```text
./bob/bob test
```

If you are satisfied with your change and want to submit for review,
create a new git commit and then push it with the following:

```text
git push origin HEAD:refs/for/master
```

> **Note:** Please follow the
[guidelines for contributors](#Submitting-Contributions)
before you push your change for review.

## How Can I Contribute?

### Reporting Bugs

This section guides you through submitting a bug report for {% Project Name %}.
Following these guidelines helps maintainers and the community understand your
report, reproduce the behavior, and find related reports.

Before creating bug reports, please check
[this list](#Before-Submitting-A-Bug-Report) as you might find out that you
 don't need to create one. When you are creating a bug report,
 please [include as many details as possible](#How-Do-I-Submit-A-Good_Bug-Report).

> **Note:** If you find a **Closed** issue that seems like it is the same
thing that you're experiencing, open a new issue and include a link to
the original issue in the body of your new one.

#### Before Submitting A Bug Report

- **Check the [Service User Guide](link).** You might be able to find
 the cause of the problem and fix things yourself.
- **Check the [FAQs on the forum][forum]** for a list of
 common questions and problems.
- **Perform a search in {% Project JIRA %}**  to see if the problem has already been
reported. If it has **and the issue is still open**, add a comment to the
existing issue instead of opening a new one.
- **Perform a search in the [discussion forum][forum]** for the project to
see if that bug was discussed before.
If not, **consider starting a new thread** to get a quick preliminary
feedback from the project maintainers.

#### How Do I Submit A (Good) Bug Report?

Bugs are tracked as {% Project JIRA %}. Select the correct component and create
an issue on it providing the following information.

Explain the problem and include additional details to help maintainers reproduce
the problem:

- **Use a clear and descriptive title** for the issue to identify the problem.
- **Describe the exact steps which reproduce the problem** in as many
details as possible.
- **Include details about your configuration and environment**.
- **Describe the behavior you observed** and point out what exactly is
the problem with that behavior.
- **Explain which behavior you expected to see instead and why.**
- **If the problem wasn't triggered by a specific action**, describe what you
were doing before the problem happened.

### Suggesting Features

This section guides you through submitting an enhancement suggestion, including
completely new features and minor improvements to existing functionality.
Following these guidelines helps maintainers and the community understand your
suggestion and find related suggestions.

Before creating feature suggestions, please check
[this list](#Before-Submitting-A-Feature-Suggestion) as you might find out
that you don't need to create one. When you are creating a feature suggestion,
please [include as many details as possible](#How-Do-I-Submit-A-Good_Feature-Suggestion).

#### Before Submitting A Feature Suggestion

- **Check the [Service User Guide](link)** for tips — you might discover
that the feature is already available.
- **Perform a search in {% Project JIRA %}** to see if the feature has already been
suggested. If it has, add a comment to the existing issue instead of
opening a new one.
- **Perform a search in the [discussion forum][forum]** for the project to
see if that enhancement was discussed before.
If not, **consider starting a new thread** to get a quick preliminary
feedback from the project maintainers.

#### How Do I Submit A (Good) Feature Suggestion?

Feature suggestions are tracked as {% Project JIRA %}. Select the correct
component and create an issue on it providing the following information:

- **Use a clear and descriptive title** for the issue to identify
the suggestion.
- **Provide a step-by-step description of the suggested feature** in as many
details as possible.
- **Explain why this feature would be useful** to most users of the service.

### Submitting Contributions

This section guides you through submitting your own contribution, including bug
fixing, new features or any kind of improvement on the content of this
repository. The process described here has several goals:

- Maintain the project's quality
- Fix problems that are important to users
- Engage the community in working toward the best possible solution
- Enable a sustainable system for project's maintainers to review contributions

#### Before Submitting A Contribution

- **Engage the project maintainers** in the proper way so that they are prepared
  to receive your contribution and can provide valuable suggestion on the design
  choices. Follow the guidelines to [report a bug](#Reporting-Bugs) or to
  [propose an enhancement](#Suggesting-Features).

#### How Do I Submit A (Good) Contribution?

Please follow these steps to have your contribution considered by the
maintainers:

- **Follow the [styleguides](#Styleguides)** when implementing your change.
- **Provide a proper description of the change and the reason for it**,
referring to the associated JIRA issue if it exists.
- **Provide proper automatic tests to verify your change**, extending the
existing test suites or creating new ones in case of new features.
- **Update the project documentation if needed**. In case of new features,
they shall be properly described in the the relevant documentation.
- After you submit your contribution, **verify that the automatic
[CI pipeline][pipeline] for your change is passing**.
If the CI pipeline is failing, and you believe that the failure is unrelated to
your change, please leave a comment on the change request explaining why you
believe the failure is unrelated. A maintainer will re-run the pipeline for you.
If we conclude that the failure was a false positive, then we will open an issue
to track that problem.

While the prerequisites above must be satisfied prior to having your pull
request reviewed, the reviewer(s) may ask you to complete additional design
work, tests, or other changes before your change request can be ultimately
accepted.

## Styleguides

### Git Commit Messages

- Respect the [commit message Design Rule][commit-dr].
- ...

### {% Code %} Styleguide

- ...

### Documentation Styleguide

- ...

[jira]: https://eteamproject.internal.ericsson.com/projects/ADPPRG/issues
[forum]: https://teams.microsoft.com/l/channel/19%3aed0a261d69df4fcda3e700b9b0938d3c%40thread.skype/General?groupId=f7576b61-67d8-4483-afea-3f6e754486ed&tenantId=92e84ceb-fbfd-47ab-be52-080c6b87953f
[bob]: https://gerrit.ericsson.se/plugins/gitiles/adp-cicd/bob/
[pipeline]: https://fem008-eiffel007.rnd.ki.sw.ericsson.se:8443/jenkins/view/ADP-Ref-App/job/adp-ref-catfacts-text-analyzer-precodereview-pipeline/
[commit-dr]: https://confluence.lmera.ericsson.se/display/AA/Artifact+handling+design+rules
