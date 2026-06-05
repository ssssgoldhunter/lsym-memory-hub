// 环境数据缓存
let environments = [];

// 页面加载
document.addEventListener('DOMContentLoaded', function () {
    loadEnvironments();
    setDefaultDate();
});

// 加载环境列表
async function loadEnvironments() {
    try {
        const resp = await fetch('/api/envs');
        const json = await resp.json();
        if (json.success) {
            environments = json.data;
            populateEnvSelects();
            renderEnvInfo();
        }
    } catch (e) {
        console.error('加载环境失败:', e);
    }
}

// 填充环境选择下拉框
function populateEnvSelects() {
    const selects = ['acct-env', 'trans24-env', 'trans25-env', 'status-env', 'transfer-env', 'download-env'];
    selects.forEach(id => {
        const sel = document.getElementById(id);
        sel.innerHTML = '';
        environments.forEach(env => {
            const opt = document.createElement('option');
            opt.value = env.id;
            opt.textContent = env.name;
            sel.appendChild(opt);
        });
    });
}

// 判断环境类型（LSYM / MDL）和场景（测试 / 生产）
function getEnvTags(envId) {
    const isTest = envId.includes('test');
    const brand = envId.startsWith('mdl') ? 'MDL' : 'LSYM';
    return {
        envLabel: isTest ? '测试' : '生产',
        envClass: isTest ? 'env-test' : 'env-prod',
        brand: brand,
        brandClass: brand === 'MDL' ? 'env-brand-mdl' : 'env-brand-lsym'
    };
}

// 渲染环境信息
function renderEnvInfo() {
    const container = document.getElementById('env-info');
    container.innerHTML = environments.map(env => {
        const tags = getEnvTags(env.id);
        return `
        <div class="col-md-3">
            <div class="card">
                <div class="card-body py-2">
                    <h6>
                        <span class="env-badge ${tags.brandClass}">${tags.brand}</span>
                        <span class="env-badge ${tags.envClass}">${tags.envLabel}</span>
                    </h6>
                    <table class="table table-sm table-borderless mb-0">
                        <tr><td class="text-muted" style="width:80px">会员编号:</td><td><code>${env.mchntMbrId}</code></td></tr>
                        <tr><td class="text-muted">API:</td><td><code style="font-size:11px">${env.url.includes('test') ? '测试' : '生产'}</code></td></tr>
                    </table>
                </div>
            </div>
        </div>`;
    }).join('');
}

// 设置默认日期为今天
function setDefaultDate() {
    const today = new Date().toISOString().split('T')[0];
    document.getElementById('trans24-date').value = today;
    document.getElementById('trans25-date').value = today;
    document.getElementById('status-date').value = today;
}

// 格式化日期为 YYYYMMDD
function formatDate(dateStr) {
    return dateStr.replace(/-/g, '');
}

// 通用查询函数
async function doQuery(url, body, resultId, spinnerId, btnId) {
    const resultEl = document.getElementById(resultId);
    const spinnerEl = document.getElementById(spinnerId);
    const btnEl = document.getElementById(btnId);

    spinnerEl.classList.remove('d-none');
    btnEl.disabled = true;
    resultEl.textContent = '查询中...';

    try {
        const resp = await fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        const json = await resp.json();

        if (json.success) {
            resultEl.textContent = JSON.stringify(json.data, null, 2);
        } else {
            resultEl.textContent = '❌ ' + json.message;
        }
    } catch (e) {
        resultEl.textContent = '❌ 请求异常: ' + e.message;
    } finally {
        spinnerEl.classList.add('d-none');
        btnEl.disabled = false;
    }
}

// 账户查询
function queryAcctInfo() {
    const body = {
        envId: document.getElementById('acct-env').value,
        acctNo: document.getElementById('acct-no').value.trim(),
        registerAttr: document.getElementById('acct-register-attr').value
    };

    if (!body.acctNo) {
        alert('请输入J账号');
        return;
    }

    doQuery('/api/query/acct-info', body, 'acct-result', 'spinner-acct', 'btn-acct');
}

// 24交易查询
function queryTransDetail24() {
    const dateVal = document.getElementById('trans24-date').value;
    const body = {
        envId: document.getElementById('trans24-env').value,
        transDate: formatDate(dateVal),
        acctNo: document.getElementById('trans24-acct-no').value.trim(),
        transType: document.getElementById('trans24-type').value,
        registerAttr: document.getElementById('trans24-register-attr').value,
        page: parseInt(document.getElementById('trans24-page').value) || 1
    };

    if (!body.acctNo) {
        alert('请输入J账号');
        return;
    }
    if (!body.transDate) {
        alert('请选择日期');
        return;
    }

    doQuery('/api/query/trans-detail-24', body, 'trans24-result', 'spinner-trans24', 'btn-trans24');
}

// 25交易查询
function queryTransDetail25() {
    const dateVal = document.getElementById('trans25-date').value;
    const body = {
        envId: document.getElementById('trans25-env').value,
        transDate: formatDate(dateVal),
        transType: document.getElementById('trans25-type').value,
        page: parseInt(document.getElementById('trans25-page').value) || 1
    };

    if (!body.transDate) {
        alert('请选择日期');
        return;
    }

    doQuery('/api/query/trans-detail-25', body, 'trans25-result', 'spinner-trans25', 'btn-trans25');
}

// 交易状态查询 - 联动切换字段
function toggleStatusFields() {
    const queryType = document.getElementById('status-query-type').value;
    document.getElementById('pay-withdraw-fields').style.display = queryType === '74_withdraw' ? '' : 'none';
    document.getElementById('pay-transfer-fields').style.display = queryType === '74_transfer' ? '' : 'none';
    document.getElementById('withdraw-fields').style.display = queryType === '87' ? '' : 'none';
}

// 交易状态查询
function queryTransStatus() {
    const queryType = document.getElementById('status-query-type').value;
    const dateVal = document.getElementById('status-date').value;
    const body = {
        envId: document.getElementById('status-env').value,
        acctNo: document.getElementById('status-acct-no').value.trim(),
        oriTransDate: formatDate(dateVal)
    };

    if (!body.acctNo) {
        alert('请输入J账号');
        return;
    }
    if (!body.oriTransDate) {
        alert('请选择原始交易日期');
        return;
    }

    if (queryType === '74_withdraw') {
        // 74-提现查询: 只需 BUSS_ID
        body.queryType = '74';
        body.bussId = document.getElementById('status-wd-buss-id').value.trim();
        if (!body.bussId) {
            alert('请输入 BUSS_ID');
            return;
        }
    } else if (queryType === '74_transfer') {
        // 74-交易查询: BUSS_ID + BUSS_SUB_ID + TRANS_TYPE
        body.queryType = '74';
        body.bussId = document.getElementById('status-tf-buss-id').value.trim();
        body.bussSubId = document.getElementById('status-tf-buss-sub-id').value.trim();
        body.transType = document.getElementById('status-tf-trans-type').value;
        if (!body.bussId) {
            alert('请输入 BUSS_ID');
            return;
        }
        if (!body.bussSubId) {
            alert('交易查询需要输入 BUSS_SUB_ID');
            return;
        }
    } else {
        // 87-提现状态查询
        body.queryType = '87';
        body.oriTransSsn = document.getElementById('status-ori-trans-ssn').value.trim();
        body.transAmt = document.getElementById('status-trans-amt').value.trim();
        body.timeStampe = document.getElementById('status-timestamp').value.trim();
        body.accountType = '1';
    }

    doQuery('/api/query/trans-status', body, 'status-result', 'spinner-status', 'btn-status');
}

// 复制结果
function copyResult(elementId) {
    const el = document.getElementById(elementId);
    navigator.clipboard.writeText(el.textContent).then(() => {
        // 简单反馈
        const original = el.style.border;
        el.style.border = '2px solid #198754';
        setTimeout(() => { el.style.border = original; }, 500);
    });
}

// ========== 转账功能 ==========

// 金额实时换算（分→元）
document.addEventListener('DOMContentLoaded', function () {
    const amtInput = document.getElementById('transfer-amt');
    if (amtInput) {
        amtInput.addEventListener('input', function () {
            const fen = this.value.trim();
            const yuanEl = document.getElementById('transfer-amt-yuan');
            if (fen && /^\d+$/.test(fen)) {
                const yuan = (parseInt(fen, 10) / 100).toFixed(2);
                yuanEl.textContent = '≈ ' + yuan + ' 元';
            } else {
                yuanEl.textContent = '';
            }
        });
    }
});

// 转账开关切换
function onTransferSwitchChange(el) {
    const badge = document.getElementById('switch-status-badge');
    const btn = document.getElementById('btn-transfer');

    if (el.checked) {
        // 开关打开 → 显示确认弹窗
        const envEl = document.getElementById('transfer-env');
        const envName = envEl.options[envEl.selectedIndex] ? envEl.options[envEl.selectedIndex].text : '';
        const payAct = document.getElementById('transfer-pay-act').value.trim();
        const recAct = document.getElementById('transfer-rec-act').value.trim();
        const payName = document.getElementById('transfer-pay-name').value.trim();
        const recName = document.getElementById('transfer-rec-name').value.trim();
        const amt = document.getElementById('transfer-amt').value.trim();
        const bussId = document.getElementById('transfer-buss-id').value.trim();
        const bussSubId = document.getElementById('transfer-buss-sub-id').value.trim();
        const memo = document.getElementById('transfer-memo').value.trim();

        // 校验必填项
        if (!payAct) { alert('请输入付款账号'); el.checked = false; return; }
        if (!recAct) { alert('请输入收款账号'); el.checked = false; return; }
        if (!amt) { alert('请输入交易金额'); el.checked = false; return; }
        if (!bussId) { alert('请输入商户订单号'); el.checked = false; return; }
        if (!bussSubId) { alert('请输入商户子订单号'); el.checked = false; return; }

        const amtYuan = /^\d+$/.test(amt) ? (parseInt(amt, 10) / 100).toFixed(2) : '?';
        const msg = '⚠️ 请确认以下转账信息：\n\n' +
            '环境: ' + envName + '\n' +
            '付款账号: ' + payAct + '\n' +
            '付款名称: ' + (payName || '未填写') + '\n' +
            '收款账号: ' + recAct + '\n' +
            '收款名称: ' + (recName || '未填写') + '\n' +
            '金额: ' + amt + ' 分 (≈ ' + amtYuan + ' 元)\n' +
            'BUSS_ID: ' + bussId + '\n' +
            'BUSS_SUB_ID: ' + bussSubId + '\n' +
            '备注: ' + (memo || '余额转账') + '\n\n' +
            '确认信息无误，点击"确定"激活转账按钮，"取消"关闭开关。';

        if (confirm(msg)) {
            // 确认 → 激活按钮
            badge.textContent = '已开启';
            badge.className = 'badge bg-danger ms-1';
            btn.disabled = false;
        } else {
            // 取消 → 关闭开关
            el.checked = false;
            badge.textContent = '已关闭';
            badge.className = 'badge bg-secondary ms-1';
            btn.disabled = true;
        }
    } else {
        // 开关关闭
        badge.textContent = '已关闭';
        badge.className = 'badge bg-secondary ms-1';
        btn.disabled = true;
    }
}

// 执行转账
async function doTransfer() {
    const resultEl = document.getElementById('transfer-result');
    const spinnerEl = document.getElementById('spinner-transfer');
    const btnEl = document.getElementById('btn-transfer');
    const switchEl = document.getElementById('transfer-switch');

    const body = {
        envId: document.getElementById('transfer-env').value,
        payAct: document.getElementById('transfer-pay-act').value.trim(),
        recAct: document.getElementById('transfer-rec-act').value.trim(),
        payName: document.getElementById('transfer-pay-name').value.trim(),
        recName: document.getElementById('transfer-rec-name').value.trim(),
        transAmt: document.getElementById('transfer-amt').value.trim(),
        bussId: document.getElementById('transfer-buss-id').value.trim(),
        bussSubId: document.getElementById('transfer-buss-sub-id').value.trim(),
        memo: document.getElementById('transfer-memo').value.trim()
    };

    spinnerEl.classList.remove('d-none');
    btnEl.disabled = true;
    resultEl.textContent = '转账请求中...';

    try {
        const resp = await fetch('/api/transfer', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        const json = await resp.json();

        if (json.success) {
            resultEl.textContent = JSON.stringify(json.data, null, 2);
        } else {
            resultEl.textContent = '❌ ' + json.message;
        }
    } catch (e) {
        resultEl.textContent = '❌ 请求异常: ' + e.message;
    } finally {
        spinnerEl.classList.add('d-none');
        // 操作完成后自动关闭开关
        switchEl.checked = false;
        switchEl.dispatchEvent(new Event('change'));
        document.getElementById('switch-status-badge').textContent = '已关闭';
        document.getElementById('switch-status-badge').className = 'badge bg-secondary ms-1';
        btnEl.disabled = true;
    }
}

// ========== 文件下载功能 ==========

// 切换下载类型字段
function toggleDownloadFields() {
    const bizFunc = document.getElementById('download-bizfunc').value;
    document.getElementById('dl-01-fields').style.display = bizFunc === '01' ? '' : 'none';
    document.getElementById('dl-02-fields').style.display = bizFunc === '02' ? '' : 'none';
    document.getElementById('dl-14-fields').style.display = bizFunc === '14' ? '' : 'none';
}

// 执行文件下载（浏览器原生下载）
async function doDownload() {
    const resultEl = document.getElementById('download-result');
    const spinnerEl = document.getElementById('spinner-download');
    const btnEl = document.getElementById('btn-download');
    const bizFunc = document.getElementById('download-bizfunc').value;

    const body = {
        envId: document.getElementById('download-env').value,
        bizFunc: bizFunc
    };

    // 根据bizFunc收集参数
    if (bizFunc === '01') {
        body.transType = document.getElementById('dl-01-transtype').value;
    } else if (bizFunc === '02') {
        body.userSsn = document.getElementById('dl-02-userssn').value.trim();
        body.userTransDt = document.getElementById('dl-02-transdt').value.trim();
        body.transType02 = document.getElementById('dl-02-transtype').value;
        if (!body.userSsn) { alert('请输入USER_SSN'); return; }
        if (!body.userTransDt) { alert('请输入交易日期'); return; }
    } else if (bizFunc === '14') {
        body.acctNo = document.getElementById('dl-14-acctno').value.trim();
        body.transStartDate = document.getElementById('dl-14-startdate').value.trim();
        body.transEndDate = document.getElementById('dl-14-enddate').value.trim();
        body.flag = document.getElementById('dl-14-flag').value;
        if (!body.acctNo) { alert('请输入用户编号'); return; }
        if (!body.transStartDate) { alert('请输入交易起始日期'); return; }
        if (!body.transEndDate) { alert('请输入交易结束日期'); return; }
    }

    spinnerEl.classList.remove('d-none');
    btnEl.disabled = true;
    resultEl.textContent = '下载请求中...';

    try {
        const resp = await fetch('/api/download', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });

        const contentType = resp.headers.get('Content-Type') || '';

        if (contentType.includes('application/json')) {
            // 返回JSON说明失败或无文件
            const json = await resp.json();
            if (json.success) {
                resultEl.textContent = JSON.stringify(json.data, null, 2);
            } else {
                resultEl.textContent = '❌ ' + json.message;
            }
        } else {
            // 返回文件流 → 触发浏览器下载
            const blob = await resp.blob();
            const disposition = resp.headers.get('Content-Disposition');
            let fileName = 'download_' + bizFunc;

            if (disposition) {
                // 优先解析 filename*=UTF-8''
                const utf8Match = disposition.match(/filename\*=UTF-8''(.+)/);
                if (utf8Match) {
                    fileName = decodeURIComponent(utf8Match[1]);
                } else {
                    const match = disposition.match(/filename="?([^"]+)"?/);
                    if (match) fileName = match[1];
                }
            }

            // 创建临时链接触发下载
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = fileName;
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);
            document.body.removeChild(a);

            resultEl.textContent = '✅ 下载成功!\n文件名: ' + fileName + '\n大小: ' + (blob.size / 1024).toFixed(1) + ' KB\n时间: ' + new Date().toLocaleString();
        }
    } catch (e) {
        resultEl.textContent = '❌ 请求异常: ' + e.message;
    } finally {
        spinnerEl.classList.add('d-none');
        btnEl.disabled = false;
    }
}
