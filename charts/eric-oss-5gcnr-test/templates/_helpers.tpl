{{/* vim: set filetype=mustache: */}}

{{- define "eric-oss-5gcnr-test.global" }}
  {{- $globalDefaults := get (fromYaml (.Files.Get "global_defaults.yaml")) "global" }}
  {{- if .Values.global }}
    {{- mergeOverwrite $globalDefaults .Values.global | toJson }}
  {{- else }}
    {{- $globalDefaults | toJson }}
  {{- end }}
{{- end }}

{{- define "eric-oss-5gcnr-test.name" }}
  {{- .Chart.Name | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "eric-oss-5gcnr-test.chart" }}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-"  }}
{{- end }}

{{- define "eric-oss-5gcnr-test.labels" }}
app.kubernetes.io/name: {{ .Values.service.name }}
app.kubernetes.io/instance: {{ .Values.service.instance }}
helm.sh/chart: {{ include "eric-oss-5gcnr-test.chart" . }}
{{- end }}

{{- define "eric-oss-5gcnr-test.pullSecret" }}
{{- $global := fromJson (include "eric-oss-5gcnr-test.global" .) -}}
{{- if $global.pullSecret }}
imagePullSecrets:
  - name: {{ $global.pullSecret | quote }}
{{- end }}
{{- end }}

{{- define "eric-oss-5gcnr-test.productStaging" }}
{{- $global := fromJson (include "eric-oss-5gcnr-test.global" .) -}}
{{- if $global.productStaging }}
{{- print "PRODUCT" }}
{{- else }}
{{- print "SERVICE" }}
{{- end }}
{{- end }}

{{- define "eric-oss-5gcnr-test.image" -}}
  {{- $productInfo := fromYaml (.Files.Get "eric-product-info.yaml") -}}
  {{- $productInfo = index $productInfo "images" "eric-oss-cnr5gassist-test" -}}
  {{- $global := fromJson (include "eric-oss-5gcnr-test.global" .) -}}
  {{- if $global.registry.url -}}
    {{- $_ := set $productInfo "registry" $global.registry.url -}}
  {{- end -}}
  {{- if $productInfo.repoPath -}}
      {{- $_ := set $productInfo "repoPath" (printf "%s/" $productInfo.repoPath) -}}
  {{- end -}}
  {{- printf "%s/%s%s:%s" $productInfo.registry $productInfo.repoPath $productInfo.name $productInfo.tag -}}
{{- end -}}
