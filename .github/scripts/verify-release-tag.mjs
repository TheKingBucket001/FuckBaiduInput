import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { setTimeout as sleep } from 'node:timers/promises';

const SHA_PATTERN = /^[0-9a-f]{40}$/;
const SSH_SIGNATURE_PATTERN = /^-----BEGIN SSH SIGNATURE-----\r?\n(?:[A-Za-z0-9+/=]+\r?\n)+-----END SSH SIGNATURE-----$/;
const PENDING_REASONS = new Set(['pending', 'gpgverify_error', 'gpgverify_unavailable']);

class RetryableGateError extends Error {}

function configuration(env) {
  const repository = env.GITHUB_REPOSITORY;
  const tagName = env.GITHUB_REF_NAME;
  const expectedCommit = env.GITHUB_SHA;
  if (!/^[A-Za-z0-9_.-]+\/[A-Za-z0-9_.-]+$/.test(repository ?? '')) {
    throw new Error('GITHUB_REPOSITORY must identify one owner/repository.');
  }
  if (!/^v\d+\.\d+\.\d+$/.test(tagName ?? '')) {
    throw new Error('GITHUB_REF_NAME must be a release tag in the form vX.Y.Z.');
  }
  if (env.GITHUB_REF_TYPE && env.GITHUB_REF_TYPE !== 'tag') {
    throw new Error('Release verification requires a tag ref.');
  }
  if (!SHA_PATTERN.test(expectedCommit ?? '')) {
    throw new Error('GITHUB_SHA must be the expected 40-character commit SHA.');
  }
  if (typeof env.GH_TOKEN !== 'string' || !env.GH_TOKEN.trim()) {
    throw new Error('GH_TOKEN is required to check the remote release tag.');
  }
  const apiUrl = new URL(env.GITHUB_API_URL ?? 'https://api.github.com');
  if (apiUrl.protocol !== 'https:' || apiUrl.username || apiUrl.password || apiUrl.search || apiUrl.hash) {
    throw new Error('GITHUB_API_URL must be an HTTPS API base URL.');
  }
  return { repository, tagName, expectedCommit, apiUrl: apiUrl.href.replace(/\/$/, ''), token: env.GH_TOKEN };
}

/** Verify the current remote tag before building or publishing release artifacts. */
export async function verifyReleaseTag({
  env = process.env,
  fetchImpl = fetch,
  sleepImpl = sleep,
  maxAttempts = 6,
  retryDelayMs = 5000,
} = {}) {
  const config = configuration(env);
  if (!Number.isInteger(maxAttempts) || maxAttempts < 1 || maxAttempts > 10
      || !Number.isInteger(retryDelayMs) || retryDelayMs < 0 || retryDelayMs > 15000) {
    throw new Error('Tag verification retry limits are invalid.');
  }

  async function getJson(endpoint) {
    let response;
    try {
      response = await fetchImpl(`${config.apiUrl}/repos/${config.repository}${endpoint}`, {
        headers: {
          Accept: 'application/vnd.github+json',
          Authorization: `Bearer ${config.token}`,
          'X-GitHub-Api-Version': '2022-11-28',
        },
        signal: AbortSignal.timeout(15000),
        redirect: 'error',
      });
    } catch {
      // Do not include request objects, headers or response bodies in diagnostics.
      throw new RetryableGateError('GitHub API request failed.');
    }
    if (!response.ok) {
      const message = `GitHub API rejected the tag check (HTTP ${response.status}).`;
      if (response.status === 429 || response.status >= 500) throw new RetryableGateError(message);
      throw new Error(message);
    }
    try {
      return await response.json();
    } catch {
      throw new Error('GitHub API returned invalid JSON for the tag check.');
    }
  }

  async function check() {
    const refEndpoint = `/git/ref/tags/${encodeURIComponent(config.tagName)}`;
    const ref = await getJson(refEndpoint);
    if (ref?.ref !== `refs/tags/${config.tagName}` || ref?.object?.type !== 'tag'
        || !SHA_PATTERN.test(ref?.object?.sha ?? '')) {
      throw new Error('The remote release ref must be an annotated tag; lightweight tags are forbidden.');
    }
    const tagObjectSha = ref.object.sha;
    const tag = await getJson(`/git/tags/${tagObjectSha}`);
    if (tag?.sha !== tagObjectSha || tag?.tag !== config.tagName) {
      throw new Error('The remote annotated tag identity does not match the requested release.');
    }
    if (tag?.object?.type !== 'commit' || tag?.object?.sha !== config.expectedCommit) {
      throw new Error('The remote release tag does not directly reference the expected GITHUB_SHA commit.');
    }
    const verification = tag.verification;
    const signature = typeof verification?.signature === 'string' ? verification.signature.trim() : '';
    if (signature && !SSH_SIGNATURE_PATTERN.test(signature)) {
      throw new Error('The remote release tag must use an SSH signature; GPG signatures are forbidden.');
    }
    if (verification?.verified !== true) {
      if (PENDING_REASONS.has(verification?.reason)) {
        throw new RetryableGateError('GitHub has not finished verifying the remote SSH tag.');
      }
      throw new Error('GitHub tag verification.verified is not true; release publication is forbidden.');
    }
    if (!SSH_SIGNATURE_PATTERN.test(signature)) {
      throw new Error('GitHub did not return an SSH signature for the verified release tag.');
    }
    // Detect a tag replacement during the API calls instead of accepting stale verification.
    const currentRef = await getJson(refEndpoint);
    if (currentRef?.ref !== ref.ref || currentRef?.object?.type !== 'tag'
        || currentRef?.object?.sha !== tagObjectSha) {
      throw new Error('The remote release tag changed during verification.');
    }
    return { repository: config.repository, tagName: config.tagName, tagObjectSha, commitSha: config.expectedCommit };
  }

  for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
    try {
      return await check();
    } catch (error) {
      if (!(error instanceof RetryableGateError) || attempt === maxAttempts) throw error;
      await sleepImpl(retryDelayMs);
    }
  }
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try {
    const result = await verifyReleaseTag();
    console.log(`Verified remote SSH tag ${result.repository} ${result.tagName}: ${result.tagObjectSha} -> ${result.commitSha}; verification.verified=true.`);
  } catch (error) {
    console.error(`Release tag verification failed: ${error.message}`);
    process.exitCode = 1;
  }
}
