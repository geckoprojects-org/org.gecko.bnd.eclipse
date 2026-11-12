# Releasing

Releases are published on **Maven Central** (`org.geckoprojects.bnd.eclipse`). Jenkins builds, signs
and uploads them; GitHub carries the tag and a GitHub release with the same files. The setup is the
one of [geckoprojects-org/paho](https://github.com/geckoprojects-org/paho).

| Branch / ref | Who | What |
|---|---|---|
| `snapshot` (development) | Jenkins | `<version>-SNAPSHOT` to the DIM Nexus and the Maven Central snapshot repository |
| `main` | Jenkins | the release: signed bundles to the DIM Nexus and, as one deployment, to the Sonatype Central Portal |
| tag `v<version>` | GitHub Actions (`release.yml`) | GitHub release with the jars, poms and `.asc` signatures downloaded from Maven Central |

The version comes from `base-version` in `cnf/ext/version.bnd`.

## How it works

- `cnf/build.bnd` enables the bnd library `fennec` (`org.eclipse.fennec.bnd:org.eclipse.fennec.bnd.library`,
  version in `cnf/central.maven`). `DO_RELEASE=true` makes it build the bare version, otherwise
  `<version>-<timestamp>-SNAPSHOT`; `GPG_PASSPHRASE` switches signing and the Portal's snapshot
  repository on.
- `cnf/ext/release.bnd` decides where a release goes (`-releaserepo`):

  | Build | Repositories |
  |---|---|
  | snapshot | `Sonatype` (Portal snapshots, with GPG) and `Release` (DIM Nexus `dim-snapshot`) |
  | release (`DO_RELEASE=true`) | `Release` (DIM Nexus `dim-release`) and `Dist` (`dist/bundles`) |
  | release, `NEXUS_RELEASE=false` | `Dist` only (trial of the Central upload) |

- For a release, `Jenkinsfile` uploads `dist/bundles` with bnd's `.github/scripts/sonatype-upload.sh`
  to the Central Portal as one deployment and follows it with `sonatype-status.sh` until it is
  validated.
- Jenkins credentials: `dim-nexus` (Nexus login), `central-portal-token` (Portal user token),
  `gnupg-passphrase` and `gnupg-private-key` (signing; or `GNUPG_PASSPHRASE` and a keyring on the
  node). The `Jenkinsfile` stops with the name of whatever is missing.

A release can be tried locally without Nexus and signing:

```sh
DO_RELEASE=true NEXUS_RELEASE=false ./gradlew clean build release
ls dist/bundles/org/geckoprojects/bnd/eclipse
```

## Steps

1. **Release notes.** Complete the `## <version>` section of `CHANGELOG.md` on `snapshot`; it
   becomes the text of the GitHub release. Update version mentions in `README.md`.
2. **Release pull request** from `snapshot` to `main`. CI must be green. Merge it with a **merge
   commit**, so that `main` keeps the history of `snapshot`.
3. **Jenkins** builds `main` with `DO_RELEASE=true`: bare version, signed, to the DIM Nexus and into
   `dist/bundles`, which it uploads to the Central Portal and follows until validated.
4. **Publish in the Portal.** The deployment waits at
   [central.sonatype.com/publishing](https://central.sonatype.com/publishing) (`USER_MANAGED`);
   check it and press *Publish*. Maven Central serves it after 10 to 30 minutes.
5. **Tag** the merge commit on `main` and push the tag:
   ```
   git fetch origin
   git tag -s v<version> -m "Gecko bnd Eclipse <version>" origin/main
   git push origin v<version>
   ```
   `release.yml` waits until the version is on Maven Central (at most about 2.5 h), checks the
   downloads against Central's checksums and creates the GitHub release with the notes of step 1.
   Should it time out, run it again from the Actions tab with the tag as input.
6. **Next version.** Raise `base-version` in `cnf/ext/version.bnd` on `snapshot` (e.g. to the next
   minor version), so that snapshots no longer carry the released version.

A version that is on Maven Central cannot be replaced; a fix needs a new version.
