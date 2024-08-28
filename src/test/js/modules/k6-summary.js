/*
 * COPYRIGHT Ericsson 2021
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 */

let forEach = function (obj, callback) {
    for (let key in obj)
        if (obj.hasOwnProperty(key))
            if (callback(key, obj[key]))
                break;
}

const groupPrefix = '>';
const detailsPrefix = '<';
const succMark = '+';
const failMark = '-';
const defaultOptions = {
    indent: ' ',
    summaryTimeUnit: null,
    summaryTrendStats: null,
};

function strWidth(s) {
    let inEscSeq = false;
    let inLongEscSeq = false;
    let width = 0;
    for (let char of s.normalize('NFKC')) {
        if (char.done) break;

        if (char === '\x1b') inEscSeq = true;
        else if (inEscSeq && char === '[') inLongEscSeq = true;
        else if (inEscSeq && inLongEscSeq && char.charCodeAt(0) >= 0x40 && char.charCodeAt(0) <= 0x7e) {
            inEscSeq = false;
            inLongEscSeq = false;
        }
        else if (inEscSeq && !inLongEscSeq && char.charCodeAt(0) >= 0x40 && char.charCodeAt(0) <= 0x5f)
            inEscSeq = false;
        else if (!inEscSeq && !inLongEscSeq) width++;
    }
    return width;
}

function summarizeCheck(indent, check, decorate) {
    return (check.fails === 0) ?
        decorate(indent + succMark + ' ' + check.name) :
        decorate(
            indent +
            failMark + ' ' +
            check.name + '\n' +
            indent + ' ' +
            detailsPrefix + '  ' +
            Math.floor((100 * check.passes) / (check.passes + check.fails)) + '% ' +
            succMark + ' ' +
            check.passes + ' / ' +
            failMark + ' ' +
            check.fails
        );
}

function summarizeGroup(indent, group, decorate) {
    let i;
    let result = [];
    if (group.name !== '') {
        result.push(indent + groupPrefix + ' ' + group.name + '\n');
        indent = indent + '  ';
    }

    for (i = 0; i < group.checks.length; i++)
        result.push(summarizeCheck(indent, group.checks[i], decorate));
    if (group.checks.length > 0) result.push('');
    for (i = 0; i < group.groups.length; i++)
        Array.prototype.push.apply(result, summarizeGroup(indent, group.groups[i], decorate));

    return result;
}

function displayNameForMetric(name) {
    const subMetricPos = name.indexOf('{');
    return (subMetricPos >= 0) ?
        '{ ' + name.substring(subMetricPos + 1, name.length - 1) + ' }' :
        name;
}

function indentForMetric(name) {
    return (name.indexOf('{') >= 0) ? '  ' : '';
}

function humanizeBytes(bytes) {
    const units = ['B', 'kB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];
    const base = 1000;
    if (bytes < 10) return bytes + ' B';

    const e = Math.floor(Math.log(bytes) / Math.log(base));
    const val = Math.floor((bytes / Math.pow(base, e)) * 10 + 0.5) / 10;
    return val.toFixed(val < 10 ? 1 : 0) + ' ' + units[e | 0];
}

const unitMap = {
    s: {unit: 's', coef: 0.001},
    ms: {unit: 'ms', coef: 1},
    us: {unit: 'us', coef: 1000},
};

function toFixedNoTrailingZeros(val, prec) {
    return parseFloat(val.toFixed(prec)).toString();
}

function toFixedNoTrailingZerosTrunc(val, prec) {
    const mult = Math.pow(10, prec);
    return toFixedNoTrailingZeros(Math.trunc(mult * val) / mult, prec);
}

function humanizeGenericDuration(dur) {
    if (dur === 0) return '0s';

    if (dur < 0.001) return Math.trunc(dur * 1000000) + 'ns';
    if (dur < 1)
        return toFixedNoTrailingZerosTrunc(dur * 1000, 2) + 'us';
    if (dur < 1000)
        return toFixedNoTrailingZerosTrunc(dur, 2) + 'ms';

    let result = toFixedNoTrailingZerosTrunc((dur % 60000) / 1000, dur > 60000 ? 0 : 2) + 's';
    let rem = Math.trunc(dur / 60000);
    if (rem < 1) return result;
    result = (rem % 60) + 'm' + result;
    rem = Math.trunc(rem / 60);

    return (rem < 1) ? result : rem + 'h' + result;
}

function humanizeDuration(dur, timeUnit) {
    return (timeUnit !== '' && unitMap.hasOwnProperty(timeUnit)) ?
        (dur * unitMap[timeUnit].coef).toFixed(2) + unitMap[timeUnit].unit :
        humanizeGenericDuration(dur);
}

function humanizeValue(val, metric, timeUnit) {
    if (metric.type === 'rate')
        return (Math.trunc(val * 100 * 100) / 100).toFixed(2) + '%';

    switch (metric.contains) {
        case 'data':
            return humanizeBytes(val);
        case 'time':
            return humanizeDuration(val, timeUnit);
        default:
            return toFixedNoTrailingZeros(val, 6);
    }
}

function nonTrendMetricValueForSum(metric, timeUnit) {
    switch (metric.type) {
        case 'counter':
            return [
                humanizeValue(metric.values.count, metric, timeUnit),
                humanizeValue(metric.values.rate, metric, timeUnit) + '/s',
            ];
        case 'gauge':
            return [
                humanizeValue(metric.values.value, metric, timeUnit),
                'min=' + humanizeValue(metric.values.min, metric, timeUnit),
                'max=' + humanizeValue(metric.values.max, metric, timeUnit),
            ];
        case 'rate':
            return [
                humanizeValue(metric.values.rate, metric, timeUnit),
                succMark + ' ' + metric.values.passes,
                failMark + ' ' + metric.values.fails,
            ];
        default:
            return ['[no data]'];
    }
}

function summarizeMetrics(options, data, decorate) {
    let result = [];

    let names = [];
    let nameLenMax = 0;

    let nonTrendValues = {};
    let nonTrendValueMaxLen = 0;
    let nonTrendExtras = {};
    let nonTrendExtraMaxLens = [0, 0];

    let trendCols = {};
    const numTrendColumns = options.summaryTrendStats.length;
    const trendColMaxLens = new Array(numTrendColumns).fill(0);
    forEach(data.metrics, function (name, metric) {
        let i;
        names.push(name);
        const displayNameWidth = strWidth(indentForMetric(name) + displayNameForMetric(name));
        if (displayNameWidth > nameLenMax) nameLenMax = displayNameWidth;

        if (metric.type === 'trend') {
            let cols = [];
            for (i = 0; i < numTrendColumns; i++) {
                const tc = options.summaryTrendStats[i];
                let value = metric.values[tc];
                if (tc === 'count') value = value.toString();
                else value = humanizeValue(value, metric, options.summaryTimeUnit);
                const valLen = strWidth(value);
                if (valLen > trendColMaxLens[i]) trendColMaxLens[i] = valLen;
                cols[i] = value;
            }
            trendCols[name] = cols;
            return;
        }
        const values = nonTrendMetricValueForSum(metric, options.summaryTimeUnit);
        nonTrendValues[name] = values[0];
        const valueLen = strWidth(values[0]);
        if (valueLen > nonTrendValueMaxLen) nonTrendValueMaxLen = valueLen;
        nonTrendExtras[name] = values.slice(1);
        for (i = 1; i < values.length; i++) {
            const extraLen = strWidth(values[i]);
            if (extraLen > nonTrendExtraMaxLens[i - 1]) nonTrendExtraMaxLens[i - 1] = extraLen;
        }
    });

    names.sort();

    const getData = function (name) {
        let i;
        if (trendCols.hasOwnProperty(name)) {
            const cols = trendCols[name];
            const tmpCols = new Array(numTrendColumns);
            for (i = 0; i < cols.length; i++) {
                tmpCols[i] =
                    options.summaryTrendStats[i] + '=' +
                    decorate(cols[i]) + ' '.repeat(trendColMaxLens[i] - strWidth(cols[i]));
            }
            return tmpCols.join(' ');
        }

        const value = nonTrendValues[name];
        let fmtData = decorate(value) + ' '.repeat(nonTrendValueMaxLen - strWidth(value));

        const extras = nonTrendExtras[name];
        if (extras.length === 1) fmtData = fmtData + ' ' + decorate(extras[0]);
        else if (extras.length > 1) {
            const parts = new Array(extras.length);
            for (i = 0; i < extras.length; i++)
                parts[i] = decorate(extras[i]) + ' '.repeat(nonTrendExtraMaxLens[i] - strWidth(extras[i]));
            fmtData = fmtData + ' ' + parts.join(' ');
        }
        return fmtData;
    };

    for (let name of names) {
        const fmtIndent = indentForMetric(name);
        let fmtName = displayNameForMetric(name);
        fmtName += decorate('.'.repeat(nameLenMax - strWidth(fmtName) - strWidth(fmtIndent) + 3) + ':');
        result.push(options.indent + '  ' + fmtIndent + ' ' + fmtName + ' ' + getData(name));
    }
    return result;
}

function generateTextSummary(data, options) {
    const mergedOpts = Object.assign({}, defaultOptions, data.options, options);
    let lines = [];

    let decorate = function (text) { return text; };

    Array.prototype.push.apply(
        lines,
        summarizeGroup(mergedOpts.indent + '    ', data.root_group, decorate),
    );

    Array.prototype.push.apply(lines, summarizeMetrics(mergedOpts, data, decorate));

    return lines.join('\n').concat('\n\n');
}

exports.textSummary = generateTextSummary;