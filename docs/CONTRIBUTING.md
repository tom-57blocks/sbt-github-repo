# How to Contribute

First off, thanks for taking the time to contribute. Below are a few guidelines to follow when
contributing.

## Overall development workflow

- **Contributor** reviews the JIRA ticket before starting development and clarifies assumptions
  with **Project Manager** if needed
- **Contributor** completes the code, adds/updates the unit tests, and locally carries out manual
  verification
- **Contributor** opens pull Request(s)
  and **[Service Maintainers](../README.md#service-maintenance)** are tagged as reviewers
    - Results from manual verification/testing if applicable should be shared in the pull request
    - Draft PR(s) are encouraged for receiving early feedback. Tags such as #do-not-review can be
      used to signal no intent for early feedback
- **Service Maintainers** review the PR(s). ETA for initial review is 1 business day
    - Service maintainers can be reached out to directly
      via [scala-tools-common](https://app.slack.com/client/T03J0EXDK8Q/C0484NV1FB8) Slack channel
      if there is any delay reviewing PR(s)
- **Contributor** addresses/responds to all open questions on the PR
- **Contributor** verifies that the introduced changes work in a remote testing environment and then
  proceeds to merge the PR if they do

## Testing Responsibilities

- In addition to adding/updating the unit tests, the **Contributor** is responsible for performing
  manual verification of the changes they are introducing.

  Make sure to provide the details of the carried out manual verification in the description of the
  PR to speed up the review process. The full instructions on how to do manual verification are
  found in [BUILDING.md](BUILDING.md)

## Style

- Review [Scala Style Guide](
  https://jia-co.atlassian.net/wiki/spaces/ENGINEERIN/pages/196707/Coding+standards#Code-styles)

## Submitting a Pull Request

- Review [Pull Request Guidelines](
  https://jia-co.atlassian.net/wiki/spaces/ENGINEERIN/pages/196707/Coding+standards#Pull-Request-Guidelines)

## Git guidelines

- Review [Git Guidelines](
  https://jia-co.atlassian.net/wiki/spaces/ENGINEERIN/pages/196707/Coding+standards#Git-guidelines)
