/**
 * HabitGate -> Google Sheets webhook
 *
 * 1. 新しい Google スプレッドシートを作る
 * 2. 拡張機能 > Apps Script を開く
 * 3. このファイルの中身を貼り付ける
 * 4. デプロイ > 新しいデプロイ > 種類: ウェブアプリ
 * 5. 実行ユーザー: 自分 / アクセスできるユーザー: 全員
 * 6. 発行された Web App URL を Android アプリの「連携URL」に保存する
 */
function doPost(e) {
  const lock = LockService.getScriptLock();
  lock.waitLock(30000);
  try {
    const ss = SpreadsheetApp.getActiveSpreadsheet();
    const sheet = ss.getSheetByName('records') || ss.insertSheet('records');
    ensureHeader_(sheet);

    const body = JSON.parse(e.postData.contents || '{}');
    const records = body.records || [];
    const rows = records.map(function (r) {
      return [
        new Date(),
        body.source || 'HabitGate',
        r.local_id || '',
        r.category || '',
        r.title || '',
        Number(r.duration_minutes || 0),
        minutesToHHMM_(Number(r.duration_minutes || 0)),
        r.actual_date || '',
        r.created_at ? new Date(Number(r.created_at)) : ''
      ];
    });

    if (rows.length > 0) {
      sheet.getRange(sheet.getLastRow() + 1, 1, rows.length, rows[0].length).setValues(rows);
    }

    return ContentService
      .createTextOutput(JSON.stringify({ ok: true, inserted: rows.length }))
      .setMimeType(ContentService.MimeType.JSON);
  } finally {
    lock.releaseLock();
  }
}

function ensureHeader_(sheet) {
  if (sheet.getLastRow() === 0) {
    sheet.appendRow([
      'synced_at',
      'source',
      'local_id',
      'category',
      'title',
      'duration_minutes',
      'duration_hhmm',
      'actual_date',
      'created_at'
    ]);
    sheet.setFrozenRows(1);
  }
}

function minutesToHHMM_(minutes) {
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return h + ':' + String(m).padStart(2, '0');
}
