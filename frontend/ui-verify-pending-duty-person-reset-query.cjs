const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();
  const result = { success: false };
  try {
    await page.goto('http://127.0.0.1:5173/archive-management/pending-archive/query', {
      waitUntil: 'networkidle',
      timeout: 120000
    });

    const expandBtn = page.getByRole('button', { name: /更多筛选条件|收起筛选条件/ });
    if (await expandBtn.isVisible()) {
      const txt = (await expandBtn.textContent()) || '';
      if (txt.includes('更多筛选条件')) {
        await expandBtn.click();
      }
    }

    const docTypeField = page.locator('.f02-field').filter({ hasText: '文档类型' }).first();
    await docTypeField.locator('.el-input').click();
    await page.locator('.el-select-dropdown__item').first().click();

    const dutyField = page.locator('.f02-field').filter({ hasText: '归档责任人' }).first();
    const dutyInput = dutyField.locator('input').first();
    await dutyInput.fill('zhangsan 10000112');

    let queryPayload = null;
    page.on('request', (req) => {
      if (req.url().includes('/api/archive-management/pending-documents/query') && req.method() === 'POST') {
        try {
          queryPayload = JSON.parse(req.postData() || '{}');
        } catch {
          queryPayload = { _raw: req.postData() };
        }
      }
    });

    await page.getByRole('button', { name: '查询' }).click();
    await page.waitForTimeout(1200);

    await page.getByRole('button', { name: '重置' }).click();
    await page.waitForTimeout(400);
    const dutyAfterReset = await dutyInput.inputValue();

    result.success = true;
    result.querySentDutyPerson = queryPayload && queryPayload.dutyPerson;
    result.resetDutyPersonCleared = dutyAfterReset === '';
    result.pass = result.querySentDutyPerson === 'zhangsan 10000112' && result.resetDutyPersonCleared;
  } catch (error) {
    result.error = String(error);
    try {
      await page.screenshot({ path: 'ui-verify-pending-duty-person-error.png', fullPage: true });
    } catch {}
  } finally {
    await browser.close();
    console.log(JSON.stringify(result, null, 2));
  }
})();
