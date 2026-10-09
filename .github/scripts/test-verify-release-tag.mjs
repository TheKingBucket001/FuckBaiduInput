import assert from 'node:assert/strict';
import test from 'node:test';
import { verifyReleaseTag } from './verify-release-tag.mjs';

const commitSha = 'a'.repeat(40);
const tagObjectSha = 'b'.repeat(40);
const sshSignature = '-----BEGIN SSH SIGNATURE-----\nU1NIU0lH\n-----END SSH SIGNATURE-----\n';
const env = {
  GITHUB_REPOSITORY: 'example/project',
  GITHUB_REF_NAME: 'v0.8.2',
  GITHUB_REF_TYPE: 'tag',
  GITHUB_SHA: commitSha,
  GH_TOKEN: 'unit-test-secret-never-log',
};

function remoteTag() {
  return {
    sha: tagObjectSha,
    tag: env.GITHUB_REF_NAME,
    object: { type: 'commit', sha: commitSha },
    verification: { verified: true, reason: 'valid', signature: sshSignature },
  };
}

function fixture({ tag = remoteTag(), refType = 'tag', responses = [] } = {}) {
  const requests = [];
  const delays = [];
  const fetchImpl = async (url, options) => {
    requests.push({ url, options });
    const override = responses.shift();
    if (override) {
      if (override instanceof Error) throw override;
      return override;
    }
    const body = url.endsWith(`/git/tags/${tagObjectSha}`)
      ? tag
      : { ref: `refs/tags/${env.GITHUB_REF_NAME}`, object: { type: refType, sha: tagObjectSha } };
    return { ok: true, status: 200, json: async () => body };
  };
  return {
    requests,
    delays,
    options: { env, fetchImpl, sleepImpl: async (ms) => { delays.push(ms); }, maxAttempts: 3, retryDelayMs: 10 },
  };
}

test('accepts only a remotely verified SSH annotated tag at the expected commit', async () => {
  const f = fixture();
  assert.deepEqual(await verifyReleaseTag(f.options), {
    repository: env.GITHUB_REPOSITORY, tagName: env.GITHUB_REF_NAME, tagObjectSha, commitSha,
  });
  assert.equal(f.requests.length, 3);
  assert.equal(f.requests[0].options.headers.Authorization, `Bearer ${env.GH_TOKEN}`);
  assert.ok(f.requests.every(({ url }) => !url.includes(env.GH_TOKEN)));
  assert.ok(f.requests.every(({ options }) => options.redirect === 'error'));
  assert.equal(f.delays.length, 0);
});

test('rejects lightweight tags immediately', async () => {
  const f = fixture({ refType: 'commit' });
  await assert.rejects(verifyReleaseTag(f.options), /annotated tag/);
  assert.equal(f.requests.length, 1);
  assert.equal(f.delays.length, 0);
});

test('rejects a verified GPG tag immediately', async () => {
  const tag = remoteTag();
  tag.verification.signature = '-----BEGIN PGP SIGNATURE-----\nZ3Bn\n-----END PGP SIGNATURE-----';
  const f = fixture({ tag });
  await assert.rejects(verifyReleaseTag(f.options), /SSH signature/);
  assert.equal(f.delays.length, 0);
});

test('rejects missing or malformed SSH signatures even when verified is true', async () => {
  for (const signature of [null, '', '-----BEGIN SSH SIGNATURE-----\nnot a signature\n-----END SSH SIGNATURE-----']) {
    const tag = remoteTag();
    tag.verification.signature = signature;
    await assert.rejects(verifyReleaseTag(fixture({ tag }).options), /SSH signature/);
  }
});

test('rejects wrong commits, nested tags and mismatched tag identities', async () => {
  for (const mutate of [
    (tag) => { tag.object.sha = 'c'.repeat(40); },
    (tag) => { tag.object.type = 'tag'; },
    (tag) => { tag.tag = 'v0.8.1'; },
    (tag) => { tag.sha = 'c'.repeat(40); },
  ]) {
    const tag = remoteTag();
    mutate(tag);
    const f = fixture({ tag });
    await assert.rejects(verifyReleaseTag(f.options), /expected GITHUB_SHA|identity/);
    assert.equal(f.delays.length, 0);
  }
});

test('requires the literal boolean true and rejects permanently unverified tags', async () => {
  for (const verified of [false, 'true', 1, null, undefined]) {
    const tag = remoteTag();
    tag.verification.verified = verified;
    tag.verification.reason = 'unknown_key';
    const f = fixture({ tag });
    await assert.rejects(verifyReleaseTag(f.options), /verification.verified is not true/);
    assert.equal(f.delays.length, 0);
  }
});

test('retries pending verification but only succeeds once all SSH gates pass', async () => {
  const tag = remoteTag();
  tag.verification = { verified: false, reason: 'pending', signature: sshSignature };
  const f = fixture({ tag });
  f.options.sleepImpl = async (ms) => {
    f.delays.push(ms);
    tag.verification.verified = true;
    tag.verification.reason = 'valid';
  };
  await verifyReleaseTag(f.options);
  assert.deepEqual(f.delays, [10]);
  assert.equal(f.requests.length, 5);
});

test('fails after the bounded pending verification retry budget', async () => {
  for (const reason of ['pending', 'gpgverify_error', 'gpgverify_unavailable']) {
    const tag = remoteTag();
    tag.verification.verified = false;
    tag.verification.reason = reason;
    const f = fixture({ tag });
    await assert.rejects(verifyReleaseTag(f.options), /not finished verifying/);
    assert.equal(f.requests.length, 6);
    assert.deepEqual(f.delays, [10, 10]);
  }
});

test('rejects remote ref replacement during verification', async () => {
  const f = fixture();
  const fetchImpl = f.options.fetchImpl;
  f.options.fetchImpl = async (...args) => {
    const response = await fetchImpl(...args);
    if (f.requests.length === 3) {
      return { ok: true, status: 200, json: async () => ({
        ref: `refs/tags/${env.GITHUB_REF_NAME}`, object: { type: 'tag', sha: 'c'.repeat(40) },
      }) };
    }
    return response;
  };
  await assert.rejects(verifyReleaseTag(f.options), /changed during verification/);
});

test('retries temporary HTTP failures and excludes response bodies and tokens from errors', async () => {
  const f = fixture({ responses: [{ ok: false, status: 503 }] });
  await verifyReleaseTag(f.options);
  assert.deepEqual(f.delays, [10]);
  const denied = fixture({ responses: [{ ok: false, status: 401 }] });
  await assert.rejects(verifyReleaseTag(denied.options), (error) => {
    assert.match(error.message, /HTTP 401/);
    assert.ok(!error.message.includes(env.GH_TOKEN));
    return true;
  });
  assert.equal(denied.delays.length, 0);
  const network = fixture({ responses: [new Error(env.GH_TOKEN), new Error(env.GH_TOKEN), new Error(env.GH_TOKEN)] });
  await assert.rejects(verifyReleaseTag(network.options), (error) => {
    assert.equal(error.message, 'GitHub API request failed.');
    assert.ok(!error.message.includes(env.GH_TOKEN));
    return true;
  });
});

test('rejects incomplete or unsafe configuration before making any request', async () => {
  for (const changes of [
    { GITHUB_REPOSITORY: '' }, { GITHUB_REF_NAME: 'master' }, { GITHUB_REF_TYPE: 'branch' },
    { GITHUB_SHA: '' }, { GH_TOKEN: '' }, { GITHUB_API_URL: 'http://api.github.com' },
    { GITHUB_API_URL: 'https://secret@api.github.com' },
  ]) {
    const f = fixture();
    await assert.rejects(verifyReleaseTag({ ...f.options, env: { ...env, ...changes } }));
    assert.equal(f.requests.length, 0);
  }
  const f = fixture();
  await assert.rejects(verifyReleaseTag({ ...f.options, maxAttempts: 100 }), /retry limits/);
  assert.equal(f.requests.length, 0);
});
