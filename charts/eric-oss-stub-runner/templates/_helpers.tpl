{{/* vim: set filetype=mustache: */}}

{{/*
Create a map from ".Values.global" with defaults if missing in values file.
This hides defaults from values file.
*/}}
{{ define "eric-oss-stub-runner.global" }}
  {{- $globalDefaults := get (fromYaml (.Files.Get "global_defaults.yaml")) "global" }}
  {{ if .Values.global }}
    {{- mergeOverwrite $globalDefaults .Values.global | toJson -}}
  {{ else }}
    {{- $globalDefaults | toJson -}}
  {{ end }}
{{ end }}

{{/*
Expand the name of the chart.
*/}}
{{- define "eric-oss-stub-runner.name" }}
  {{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "eric-oss-stub-runner.chart" }}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create chart version as used by the chart label.
*/}}
{{- define "eric-oss-stub-runner.version" }}
{{- printf "%s" .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create image pull secret, service level parameter takes precedence
*/}}
{{- define "eric-oss-stub-runner.pullSecret" -}}
  {{- $global := fromJson (include "eric-oss-stub-runner.global" .) -}}
  {{- if .Values.imageCredentials -}}
    {{- if .Values.imageCredentials.pullSecret -}}
      {{- $_ := set $global "pullSecret" .Values.imageCredentials.pullSecret -}}
    {{- end -}}
  {{- end -}}
  {{- print $global.pullSecret -}}
{{- end -}}

{{/*
Create a merged set of nodeSelectors from global and service level.
*/}}
{{- define "eric-oss-stub-runner.nodeSelector" -}}
  {{- $global := fromJson (include "eric-oss-stub-runner.global" .) -}}
  {{- if .Values.nodeSelector }}
    {{- toYaml (mergeOverwrite $global.nodeSelector .Values.nodeSelector) }}
  {{- else }}
    {{- toYaml $global.nodeSelector }}
  {{- end }}
{{- end -}}

{{/*
Create product info.
*/}}
{{- define "eric-oss-stub-runner.product-info" }}
{{- $productInfo := fromYaml (.Files.Get "eric-product-info.yaml") }}
ericsson.com/product-name: {{ $productInfo.productName | quote }}
ericsson.com/product-number: {{ $productInfo.productNumber | quote }}
ericsson.com/product-revision: {{regexReplaceAll "(.*)[+|-].*" .Chart.Version "${1}" | quote }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "eric-oss-stub-runner.selectorLabels" -}}
app.kubernetes.io/name: {{ include "eric-oss-stub-runner.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "eric-oss-stub-runner.labels" }}
helm.sh/chart: {{ include "eric-oss-stub-runner.chart" . }}
{{ include "eric-oss-stub-runner.selectorLabels" . }}
app.kubernetes.io/version: {{ include "eric-oss-stub-runner.version" . }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{/*
Create a user defined annotation (DR-D1121-065)
*/}}
{{- define "eric-oss-stub-runner.config-annotations" }}
{{- if .Values.annotations -}}
{{- range $name, $config := .Values.annotations }}
{{ $name }}: {{ tpl $config $ }}
{{- end }}
{{- end }}
{{- end}}

{{/*
Create a user defined labels (DR-D1121-068)
*/}}
{{ define "eric-oss-stub-runner.config-labels" }}
{{- if .Values.labels -}}
{{- range $name, $config := .Values.labels }}
{{ $name }}: {{ tpl $config $ }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Define the role reference for security policy
*/}}
{{- define "eric-oss-stub-runner.securityPolicy.reference" -}}
  {{- $global := fromJson (include "eric-oss-stub-runner.global" .) }}
  {{- $reference := "default-restricted-security-policy" }}
  {{- if $global.security.policyReferenceMap -}}
    {{ $mapped := index .Values "global" "security" "policyReferenceMap" "default-restricted-security-policy" }}
    {{- if $mapped -}}
        {{ $reference = $mapped }}
    {{- end -}}
  {{- end -}}
  {{- print $reference }}
{{- end -}}

{{/*
The image path (DR-D1121-067)
*/}}
{{- define "eric-oss-stub-runner.image" -}}
  {{- $productInfo := fromYaml (.Files.Get "eric-product-info.yaml") -}}
  {{- $productInfo = index $productInfo "images" .imageName -}}
  {{- $global := fromJson (include "eric-oss-stub-runner.global" .) -}}
  {{- if $global.registry.url -}}
    {{- $_ := set $productInfo "registry" $global.registry.url -}}
  {{- end -}}
  {{- if hasKey (index .Values "imageCredentials" .imageName) "registry" }}
    {{- if hasKey (index .Values "imageCredentials" .imageName "registry") "url" }}
      {{- $_ := set $productInfo "registry" (index .Values "imageCredentials" .imageName "registry" "url") }}
    {{- end }}
  {{- end }}
  {{- if hasKey (index .Values "imageCredentials" .imageName) "repoPath" -}}
    {{- $_ := set $productInfo "repoPath" (index .Values "imageCredentials" .imageName "repoPath") -}}
  {{- end -}}
  {{- if hasKey (index .Values "images" .imageName) "name" -}}
    {{- $_ := set $productInfo "name" (index .Values "images" .imageName "name") -}}
  {{- end -}}
  {{- if hasKey (index .Values "images" .imageName) "tag" -}}
    {{- $_ := set $productInfo "tag" (index .Values "images" .imageName "tag") -}}
  {{- end -}}
  {{- if $productInfo.repoPath -}}
      {{- $_ := set $productInfo "repoPath" (printf "%s/" $productInfo.repoPath) -}}
  {{- end -}}
  {{- printf "%s/%s%s:%s" $productInfo.registry $productInfo.repoPath $productInfo.name $productInfo.tag -}}
{{- end -}}