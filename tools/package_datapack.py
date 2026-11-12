import argparse
import os
import zipfile


def package(source, output):
    source = os.path.abspath(source)
    output = os.path.abspath(output)
    if not os.path.isfile(os.path.join(source, "pack.mcmeta")):
        raise SystemExit("Falta pack.mcmeta. Ejecuta primero gen_world.py.")
    os.makedirs(os.path.dirname(output) or ".", exist_ok=True)
    with zipfile.ZipFile(output, "w", zipfile.ZIP_DEFLATED) as archive:
        for folder, _, names in os.walk(source):
            for name in names:
                path = os.path.join(folder, name)
                archive.write(path, os.path.relpath(path, source))
    print(output)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Empaqueta el datapack de AeroRuins")
    parser.add_argument("source")
    parser.add_argument("output")
    args = parser.parse_args()
    package(args.source, args.output)
