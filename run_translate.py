import re
import xml.etree.ElementTree as ET
from pathlib import Path

from translate import Translator


ROOT_DIR = Path(__file__).resolve().parent
BASE_FILE = ROOT_DIR / "app" / "src" / "main" / "res" / "values" / "strings.xml"
RES_DIR = BASE_FILE.parent.parent
LANG_CODE_OVERRIDES = {"in": "id"}
PLACEHOLDER_PATTERN = re.compile(r"%\d+\$[a-zA-Z]|%[a-zA-Z]")


def protect_placeholders(text: str) -> tuple[str, dict[str, str]]:
    replacements: dict[str, str] = {}

    def _replace(match: re.Match[str]) -> str:
        token = f"__PH_{len(replacements)}__"
        replacements[token] = match.group(0)
        return token

    return PLACEHOLDER_PATTERN.sub(_replace, text), replacements


def restore_placeholders(text: str, replacements: dict[str, str]) -> str:
    for token, value in replacements.items():
        text = text.replace(token, value)
    return text


def iter_locale_files() -> list[Path]:
    return sorted(RES_DIR.glob("values-*/strings.xml"))


def locale_dir_to_lang_code(locale_dir_name: str) -> str:
    suffix = locale_dir_name.replace("values-", "", 1)
    base_code = suffix.split("-r", 1)[0]
    return LANG_CODE_OVERRIDES.get(base_code, base_code)


def main() -> None:
    base_tree = ET.parse(BASE_FILE)

    for locale_file in iter_locale_files():
        locale_code = locale_dir_to_lang_code(locale_file.parent.name)
        print(f"Translating {locale_file} to {locale_code}...")

        tree = ET.ElementTree(ET.fromstring(ET.tostring(base_tree.getroot())))
        root = tree.getroot()
        translator = Translator(to_lang=locale_code)

        for string_elem in root.findall("string"):
            if string_elem.get("translatable") == "false":
                continue

            original_text = string_elem.text
            if not original_text or original_text.startswith("@"):
                continue

            try:
                text_to_translate, replacements = protect_placeholders(original_text)
                translated_text = translator.translate(text_to_translate)
                string_elem.text = restore_placeholders(translated_text, replacements)
            except Exception:
                print(f"Failed to translate: {original_text}")

        ET.indent(tree, space="    ")
        tree.write(locale_file, encoding="utf-8", xml_declaration=True)
        print(f"Finished {locale_file}")


if __name__ == "__main__":
    main()
