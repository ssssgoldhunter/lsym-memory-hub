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
    const selects = ['acct-env', 'trans24-env', 'trans25-env', 'status-env', 'download-env', 'refund-env'];
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
    document.getElementById('refund-transdt').value = today;
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

// ========== 交易明细CSV下载(全部页) ==========

// 24交易CSV下载
async function downloadTransDetail24Csv() {
    const body = {
        envId: document.getElementById('trans24-env').value,
        transDate: formatDate(document.getElementById('trans24-date').value),
        acctNo: document.getElementById('trans24-acct-no').value.trim(),
        transType: document.getElementById('trans24-type').value,
        registerAttr: document.getElementById('trans24-register-attr').value
    };
    if (!body.acctNo) { alert('请输入J账号'); return; }
    if (!body.transDate) { alert('请选择日期'); return; }
    await downloadTransCsv('/api/download/trans-detail-24-csv', body,
        'trans24-result', 'spinner-trans24-csv', 'btn-trans24-csv', 'trans24_' + body.transDate);
}

// 25交易CSV下载
async function downloadTransDetail25Csv() {
    const body = {
        envId: document.getElementById('trans25-env').value,
        transDate: formatDate(document.getElementById('trans25-date').value),
        transType: document.getElementById('trans25-type').value
    };
    if (!body.transDate) { alert('请选择日期'); return; }
    await downloadTransCsv('/api/download/trans-detail-25-csv', body,
        'trans25-result', 'spinner-trans25-csv', 'btn-trans25-csv', 'trans25_' + body.transDate);
}

// 通用CSV下载: JSON响应显示消息, 文件流触发浏览器下载
async function downloadTransCsv(url, body, resultId, spinnerId, btnId, defaultPrefix) {
    const resultEl = document.getElementById(resultId);
    const spinnerEl = document.getElementById(spinnerId);
    const btnEl = document.getElementById(btnId);

    spinnerEl.classList.remove('d-none');
    btnEl.disabled = true;
    resultEl.textContent = '下载中(正在查询全部页, 请稍候)...';

    try {
        const resp = await fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        const contentType = resp.headers.get('Content-Type') || '';
        if (contentType.includes('application/json')) {
            const json = await resp.json();
            resultEl.textContent = json.success ? JSON.stringify(json.data, null, 2) : '❌ ' + json.message;
        } else {
            await saveBlob(resp, defaultPrefix + '.csv', resultEl);
        }
    } catch (e) {
        resultEl.textContent = '❌ 请求异常: ' + e.message;
    } finally {
        spinnerEl.classList.add('d-none');
        btnEl.disabled = false;
    }
}

// 通用: 从文件流响应触发浏览器下载
function saveBlob(resp, defaultFileName, resultEl) {
    return resp.blob().then(blob => {
        const disposition = resp.headers.get('Content-Disposition');
        let fileName = defaultFileName;
        if (disposition) {
            const utf8Match = disposition.match(/filename\*=UTF-8''(.+)/);
            if (utf8Match) {
                fileName = decodeURIComponent(utf8Match[1]);
            } else {
                const match = disposition.match(/filename="?([^"]+)"?/);
                if (match) fileName = match[1];
            }
        }
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = fileName;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
        resultEl.textContent = '✅ 下载成功!\n文件名: ' + fileName
            + '\n大小: ' + (blob.size / 1024).toFixed(1) + ' KB\n时间: ' + new Date().toLocaleString();
    });
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
            await saveBlob(resp, 'download_' + bizFunc, resultEl);
        }
    } catch (e) {
        resultEl.textContent = '❌ 请求异常: ' + e.message;
    } finally {
        spinnerEl.classList.add('d-none');
        btnEl.disabled = false;
    }
}

// ========== 退款 (bizFunc=21) ==========
let refundPendingBody = null;

function toggleRefundBtn() {
    const enabled = document.getElementById('refund-switch').checked;
    document.getElementById('btn-refund').disabled = !enabled;
}

function toggleRefundFields() {
    const oriType = document.getElementById('refund-ori-type').value;
    document.getElementById('refund-ori-buss').style.display = oriType === 'buss' ? '' : 'none';
    document.getElementById('refund-ori-ssn').style.display = oriType === 'ssn' ? '' : 'none';
}

function queryRefund() {
    const oriType = document.getElementById('refund-ori-type').value;
    const dateVal = document.getElementById('refund-transdt').value;

    const body = {
        envId: document.getElementById('refund-env').value,
        fundTp: document.getElementById('refund-fundtp').value,
        transDt: dateVal ? dateVal.replace(/-/g, '') : '',
        transTm: document.getElementById('refund-transtm').value.trim(),
        oriUserDId: document.getElementById('refund-oriuserdid').value.trim(),
        oriUserDNm: document.getElementById('refund-oriuserdnm').value.trim(),
        oriUserCId: document.getElementById('refund-oriusercid').value.trim(),
        oriUserCNm: document.getElementById('refund-oriusercnm').value.trim(),
        oriUserCAmt: document.getElementById('refund-amt').value.trim()
    };

    if (oriType === 'buss') {
        body.oriBussId = document.getElementById('refund-oribussid').value.trim();
        body.oriBussSubId = document.getElementById('refund-oribussubid').value.trim();
        body.oriUserSsn = '';
        if (!body.oriBussId) { alert('请输入 ORI_BUSS_ID'); return; }
    } else {
        body.oriUserSsn = document.getElementById('refund-oriuserssn').value.trim();
        body.oriBussId = '';
        body.oriBussSubId = '';
        if (!body.oriUserSsn) { alert('请输入 ORI_USER_SSN'); return; }
    }
    body.oriUserTransDt = document.getElementById('refund-oriusertransdt').value.trim();
    if (!body.oriUserTransDt) { alert('请输入 ORI_USER_TRANS_DT'); return; }

    if (!body.transDt) { alert('请选择退款日期'); return; }
    if (!body.transTm) { alert('请输入退款时间'); return; }
    if (!body.oriUserDId || !body.oriUserDNm || !body.oriUserCId || !body.oriUserCNm) {
        alert('请填写完整的参与方信息');
        return;
    }
    if (!body.oriUserCAmt) { alert('请输入退款金额'); return; }

    // 保存数据，显示确认弹窗
    refundPendingBody = body;

    const envName = environments.find(e => e.id === body.envId)?.name || body.envId;
    const oriLabel = oriType === 'buss'
        ? 'ORI_BUSS_ID: ' + body.oriBussId + (body.oriBussSubId ? '<br>ORI_BUSS_SUB_ID: ' + body.oriBussSubId : '')
        : 'ORI_USER_SSN: ' + body.oriUserSsn;

    const rows = [
        ['环境', envName],
        ['资金类型 (FUND_TP)', body.fundTp],
        ['原支付标识', oriLabel],
        ['原支付中信侧日期', body.oriUserTransDt],
        ['退款日期', body.transDt],
        ['退款时间', body.transTm],
        ['付款方 (ORI_USER_D)', body.oriUserDId + ' / ' + body.oriUserDNm],
        ['收款方 (ORI_USER_C)', body.oriUserCId + ' / ' + body.oriUserCNm],
        ['<span class="text-danger fw-bold">退款金额</span>', '<span class="text-danger fw-bold">' + body.oriUserCAmt + ' 分</span>']
    ];

    const tbody = document.querySelector('#refund-confirm-table tbody');
    tbody.innerHTML = rows.map(r =>
        '<tr><td style="width:180px" class="text-muted">' + r[0] + '</td><td>' + r[1] + '</td></tr>'
    ).join('');

    const modal = new bootstrap.Modal(document.getElementById('refundConfirmModal'));
    modal.show();
}

function doRefund() {
    if (!refundPendingBody) return;

    const resultEl = document.getElementById('refund-result');
    const spinnerEl = document.getElementById('spinner-refund');
    const btnEl = document.getElementById('btn-refund');
    const modalSpinner = document.getElementById('spinner-refund-confirm');
    const confirmBtn = document.getElementById('refund-confirm-btn');

    modalSpinner.classList.remove('d-none');
    confirmBtn.disabled = true;
    spinnerEl.classList.remove('d-none');
    btnEl.disabled = true;
    resultEl.textContent = '退款请求发送中...';

    fetch('/api/refund', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(refundPendingBody)
    }).then(resp => resp.json()).then(json => {
        modalSpinner.classList.add('d-none');
        confirmBtn.disabled = false;
        // 关闭弹窗
        const modalEl = document.getElementById('refundConfirmModal');
        const modal = bootstrap.Modal.getInstance(modalEl);
        if (modal) modal.hide();

        if (json.success) {
            resultEl.textContent = JSON.stringify(json.data, null, 2);
        } else {
            resultEl.textContent = '❌ ' + json.message;
        }
    }).catch(e => {
        modalSpinner.classList.add('d-none');
        confirmBtn.disabled = false;
        resultEl.textContent = '❌ 请求异常: ' + e.message;
    }).finally(() => {
        spinnerEl.classList.add('d-none');
        btnEl.disabled = false;
    });
}

// ========== 转账 (bizFunc=27) ==========
let transferPendingBody = null;

function toggleTransferBtn() {
    const enabled = document.getElementById('transfer-switch').checked;
    document.getElementById('btn-transfer').disabled = !enabled;
}

function queryTransfer() {
    const dateVal = document.getElementById('transfer-transdt').value;
    const body = {
        envId: document.getElementById('transfer-env').value,
        fundTp: document.getElementById('transfer-fundtp').value,
        outAcctNo: document.getElementById('transfer-outacct').value.trim(),
        outAcctNm: document.getElementById('transfer-outnm').value.trim(),
        inAcctNo: document.getElementById('transfer-inacct').value.trim(),
        inAcctNm: document.getElementById('transfer-innm').value.trim(),
        transAmt: document.getElementById('transfer-amt').value.trim(),
        bussId: document.getElementById('transfer-bussid').value.trim(),
        bussSubId: document.getElementById('transfer-bussubid').value.trim(),
        transDt: dateVal ? dateVal.replace(/-/g, '') : '',
        transTm: document.getElementById('transfer-transtm').value.trim(),
        memo: document.getElementById('transfer-memo').value.trim()
    };

    if (!body.outAcctNo || !body.outAcctNm) { alert('请填写付款方信息'); return; }
    if (!body.inAcctNo || !body.inAcctNm) { alert('请填写收款方信息'); return; }
    if (!body.transAmt) { alert('请输入转账金额'); return; }
    if (!body.bussId) { alert('请输入商户订单号 BUSS_ID'); return; }
    if (!body.transDt) { alert('请选择交易日期'); return; }
    if (!body.transTm) { alert('请输入交易时间'); return; }

    transferPendingBody = body;

    const envName = environments.find(e => e.id === body.envId)?.name || body.envId;
    const rows = [
        ['环境', envName],
        ['资金类型 (FUND_TP)', body.fundTp],
        ['付款方', body.outAcctNo + ' / ' + body.outAcctNm],
        ['收款方', body.inAcctNo + ' / ' + body.inAcctNm],
        ['<span class="text-danger fw-bold">转账金额</span>', '<span class="text-danger fw-bold">' + body.transAmt + ' 分</span>'],
        ['BUSS_ID', body.bussId],
        ['BUSS_SUB_ID', body.bussSubId || '-'],
        ['交易日期', body.transDt],
        ['交易时间', body.transTm],
        ['MEMO', body.memo || '失败交易退款']
    ];

    const tbody = document.querySelector('#transfer-confirm-table tbody');
    tbody.innerHTML = rows.map(r =>
        '<tr><td style="width:160px" class="text-muted">' + r[0] + '</td><td>' + r[1] + '</td></tr>'
    ).join('');

    const modal = new bootstrap.Modal(document.getElementById('transferConfirmModal'));
    modal.show();
}

function doTransfer() {
    if (!transferPendingBody) return;

    const modalSpinner = document.getElementById('spinner-transfer-confirm');
    const confirmBtn = document.getElementById('transfer-confirm-btn');
    const spinnerEl = document.getElementById('spinner-transfer');
    const btnEl = document.getElementById('btn-transfer');
    const resultEl = document.getElementById('transfer-result');

    modalSpinner.classList.remove('d-none');
    confirmBtn.disabled = true;
    spinnerEl.classList.remove('d-none');
    btnEl.disabled = true;
    resultEl.textContent = '转账请求发送中...';

    fetch('/api/transfer', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(transferPendingBody)
    }).then(resp => resp.json()).then(json => {
        modalSpinner.classList.add('d-none');
        confirmBtn.disabled = false;
        const modalEl = document.getElementById('transferConfirmModal');
        const modal = bootstrap.Modal.getInstance(modalEl);
        if (modal) modal.hide();

        if (json.success) {
            resultEl.textContent = JSON.stringify(json.data, null, 2);
        } else {
            resultEl.textContent = '❌ ' + json.message;
        }
    }).catch(e => {
        modalSpinner.classList.add('d-none');
        confirmBtn.disabled = false;
        resultEl.textContent = '❌ 请求异常: ' + e.message;
    }).finally(() => {
        spinnerEl.classList.add('d-none');
        btnEl.disabled = false;
    });
}
