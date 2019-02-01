
Locales Editor
------

A tiny tool to for generating locales.

### Usage

![npm](https://img.shields.io/npm/v/@jimengio/locales-editor.svg)

```bash
yarn global add @jimengio/locales-editor
open http://fe.jimu.io/locales-editor/
locales-editor # in the folder contains locales.edn
```

`locales.edn` is the snapshot defined in our own project

To perform file generation only:

```bash
op=compile locales-editor
```

### Translation

Current tranlstion is using https://ai.youdao.com/docs/doc-trans-api.s#p08

Add `:app-id` and `:app-secret` are filled in `:settings`.

### Workflow

https://github.com/Cumulo/cumulo-workflow

### License

MIT
