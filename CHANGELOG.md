# Changelog

## HookForge 0.1.0 — multilingual update

- Added Vietnamese (`vi`), English (`en`) and Simplified Chinese (`cn`) language files.
- Added `/hookforge language <vi|en|cn>` and tab completion.
- Added `language` configuration setting and migration for existing installations.
- Localized command output, doctor diagnostics, config explanations and validation errors.
- Language files are copied to `plugins/HookForge/lang/` and can be edited; missing keys fall back to Vietnamese.
- Changing the language does not replace the registered HookForge API service, so consumers that cache its API instance continue to work and explanations follow the selected language.
- Applying the beginner or advanced preset preserves the selected language.
