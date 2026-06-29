/**
 * HabitGate 用 Google Sheets Webhook
 *
 * 使い方:
 * 1. Google スプレッドシートを作成
 * 2. 拡張機能 > Apps Script を開く
 * 3. このファイルの内容を貼り付ける
 * 4. デプロイ > 新しいデプロイ > ウェブアプリ
 * 5. 実行ユーザー: 自分 / アクセスできるユーザー: 全員 または リンクを知っている全員
 * 6. 発行された Web App URL を HabitGate に保存
 */
function doPost(e) {
  const lock = LockService.getScriptLock();
  lock.waitLock(30000);
  try {
    const ss = SpreadsheetApp.getActiveSpreadsheet();
    const payload = JSON.parse(e.postData.contents || '{}');

    appendRecords_(ss, payload.records || []);
    appendCycles_(ss, payload.cycles || []);

    return ContentService
      .createTextOutput(JSON.stringify({ ok: true }))
      .setMimeType(ContentService.MimeType.JSON);
  } catch (err) {
    return ContentService
      .createTextOutput(JSON.stringify({ ok: false, error: String(err) }))
      .setMimeType(ContentService.MimeType.JSON);
  } finally {
    lock.releaseLock();
  }
}

function appendRecords_(ss, records) {
  const sheet = getOrCreateSheet_(ss, 'records', [
    'received_at',
    'local_id',
    'category',
    'title',
    'note',
    'duration_minutes',
    'duration_hhmm',
    'actual_date',
    'created_at'
  ]);

  const now = new Date();
  records.forEach(function(r) {
    sheet.appendRow([
      now,
      r.local_id || '',
      r.category || '',
      r.title || '',
      r.note || '',
      r.duration_minutes || 0,
      toHHMM_(r.duration_minutes || 0),
      r.actual_date || '',
      r.created_at ? new Date(Number(r.created_at)) : ''
    ]);
  });
}

function appendCycles_(ss, cycles) {
  const sheet = getOrCreateSheet_(ss, 'cycles', [
    'received_at',
    'local_id',
    'cycle_date',
    'start_at',
    'end_at',
    'closed'
  ]);

  const now = new Date();
  cycles.forEach(function(c) {
    sheet.appendRow([
      now,
      c.local_id || '',
      c.cycle_date || '',
      c.start_at ? new Date(Number(c.start_at)) : '',
      c.end_at ? new Date(Number(c.end_at)) : '',
      c.closed ? 1 : 0
    ]);
  });
}

function getOrCreateSheet_(ss, name, headers) {
  let sheet = ss.getSheetByName(name);
  if (!sheet) sheet = ss.insertSheet(name);
  if (sheet.getLastRow() === 0) {
    sheet.appendRow(headers);
  }
  return sheet;
}

function toHHMM_(minutes) {
  const total = Math.max(0, Number(minutes) || 0);
  const h = Math.floor(total / 60);
  const m = total % 60;
  return h + ':' + String(m).padStart(2, '0');
}
