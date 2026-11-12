import argparse
import json
import os
import shutil
import subprocess
import sys
import tkinter as tk
from datetime import datetime
from tkinter import filedialog, messagebox, ttk


CONFIG_FIELDS = [
    ("caves", "Cuevas", "bool"),
    ("chests", "Cofres", "bool"),
    ("chest-rarity", "Multiplicador de cofres", "float"),
    ("mobs", "Mobs", "bool"),
    ("vanilla-structures", "Estructuras vanilla", "bool"),
    ("liminal-chance", "Probabilidad de salas liminales", "int"),
    ("world-size", "Tamano del mundo", "int"),
    ("dome-spacing", "Separacion de domos", "int"),
    ("village-chance", "Probabilidad de aldeas", "int"),
    ("town-chance", "Probabilidad de ciudades", "int"),
    ("road-spacing", "Separacion de carreteras", "int"),
    ("mob-spawn-rate", "Tasa de mobs naturales", "float"),
    ("structure-mob-rate", "Tasa de guardianes", "float"),
    ("veil-dimensions", "Dimensiones The Veil", "bool"),
    ("suburb-ceilings", "Techos del suburbio", "bool"),
    ("classic-structures", "Estructuras clasicas", "bool"),
    ("fauna-density", "Densidad de fauna", "float"),
    ("datapack-auto-download", "Descargar datapack", "bool"),
    ("datapack-url", "URL HTTPS del datapack", "string"),
    ("datapack-file-name", "Nombre del ZIP", "string"),
    ("datapack-timeout-seconds", "Tiempo limite de descarga", "int"),
    ("datapack-max-size-mb", "Tamano maximo del ZIP", "int"),
]


def parse_scalar(value):
    value = value.strip()
    if value.lower() in ("true", "false"):
        return value.lower() == "true"
    try:
        return float(value) if any(c in value for c in ".eE") else int(value)
    except ValueError:
        return value.strip("'\"")


def read_config(path):
    values = {}
    if not os.path.isfile(path):
        return values
    with open(path, encoding="utf-8") as handle:
        for line in handle:
            line = line.strip()
            if not line or line.startswith("#") or ":" not in line:
                continue
            key, value = line.split(":", 1)
            value = value.split(" #", 1)[0].strip()
            values[key.strip()] = parse_scalar(value)
    return values


def yaml_value(value):
    if isinstance(value, bool):
        return "true" if value else "false"
    return str(value)


def write_config(path, values):
    lines = [f"{key}: {yaml_value(values[key])}" for key, _, _ in CONFIG_FIELDS]
    with open(path, "w", encoding="utf-8", newline="\n") as handle:
        handle.write("\n".join(lines) + "\n")


def backup(path):
    if os.path.isfile(path):
        stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
        shutil.copy2(path, path + "." + stamp + ".bak")


def json_files(root):
    if not os.path.isdir(root):
        return []
    result = []
    for folder, _, names in os.walk(root):
        for name in names:
            if name.endswith(".json"):
                result.append(os.path.join(folder, name))
    return sorted(result)


class WorldgenEditor(tk.Tk):
    def __init__(self, root):
        super().__init__()
        self.root_dir = os.path.abspath(root)
        self.config_path = os.path.join(self.root_dir, "src", "main", "resources", "config.yml")
        self.datapack_dir = os.path.join(self.root_dir, "datapack")
        self.config_values = read_config(self.config_path)
        self.config_vars = {}
        self.json_paths = []
        self.current_json = None
        self.title("AeroRuins - Editor interactivo de World Gen")
        self.geometry("1100x720")
        self.minsize(850, 560)
        self.build_ui()
        self.load_config()
        self.refresh_jsons()

    def build_ui(self):
        notebook = ttk.Notebook(self)
        notebook.pack(fill="both", expand=True, padx=8, pady=8)
        config_tab = ttk.Frame(notebook)
        json_tab = ttk.Frame(notebook)
        notebook.add(config_tab, text="Configuracion del mundo")
        notebook.add(json_tab, text="Datapack JSON")

        fields = ttk.LabelFrame(config_tab, text="Parametros de generacion")
        fields.pack(fill="both", expand=True, padx=8, pady=8)
        for index, (key, label, kind) in enumerate(CONFIG_FIELDS):
            row, col = index % 9, (index // 9) * 2
            ttk.Label(fields, text=label).grid(row=row, column=col, sticky="w", padx=8, pady=5)
            var = tk.StringVar()
            self.config_vars[key] = (var, kind)
            if kind == "bool":
                widget = ttk.Checkbutton(fields, variable=var, onvalue="true", offvalue="false")
            else:
                widget = ttk.Entry(fields, textvariable=var, width=14)
            widget.grid(row=row, column=col + 1, sticky="ew", padx=8, pady=5)
        for column in range(6):
            fields.columnconfigure(column, weight=1)

        buttons = ttk.Frame(config_tab)
        buttons.pack(fill="x", padx=8, pady=(0, 8))
        ttk.Button(buttons, text="Guardar configuracion", command=self.save_config).pack(side="left", padx=3)
        ttk.Button(buttons, text="Regenerar biomas", command=self.generate_world).pack(side="left", padx=3)
        ttk.Button(buttons, text="Regenerar loot", command=self.generate_loot).pack(side="left", padx=3)
        ttk.Button(buttons, text="Empaquetar datapack", command=self.package_datapack).pack(side="left", padx=3)
        ttk.Button(buttons, text="Compilar plugin", command=self.build_plugin).pack(side="left", padx=3)

        left = ttk.Frame(json_tab)
        left.pack(side="left", fill="y", padx=(8, 4), pady=8)
        ttk.Button(left, text="Actualizar lista", command=self.refresh_jsons).pack(fill="x", pady=(0, 6))
        self.json_list = tk.Listbox(left, width=42, exportselection=False)
        self.json_list.pack(fill="y", expand=True)
        self.json_list.bind("<<ListboxSelect>>", self.open_json)
        right = ttk.Frame(json_tab)
        right.pack(side="left", fill="both", expand=True, padx=(4, 8), pady=8)
        ttk.Button(right, text="Guardar JSON", command=self.save_json).pack(anchor="w", pady=(0, 6))
        self.json_editor = tk.Text(right, wrap="none", undo=True)
        self.json_editor.pack(side="left", fill="both", expand=True)
        scrollbar = ttk.Scrollbar(right, orient="vertical", command=self.json_editor.yview)
        scrollbar.pack(side="right", fill="y")
        self.json_editor.configure(yscrollcommand=scrollbar.set)

        self.log = tk.Text(self, height=7, state="disabled", wrap="word")
        self.log.pack(fill="x", padx=8, pady=(0, 8))

    def load_config(self):
        for key, (var, kind) in self.config_vars.items():
            value = self.config_values.get(key, False if kind == "bool" else "")
            var.set(yaml_value(value))

    def collect_config(self):
        values = {}
        for key, (var, kind) in self.config_vars.items():
            raw = var.get().strip()
            if kind == "bool":
                if raw not in ("true", "false"):
                    raise ValueError(f"{key} debe ser true o false")
                values[key] = raw == "true"
            elif kind == "string":
                values[key] = raw
            elif kind == "int":
                values[key] = int(raw)
            else:
                values[key] = float(raw)
        if not 0 <= values["liminal-chance"] <= 100:
            raise ValueError("liminal-chance debe estar entre 0 y 100")
        if not 0 <= values["village-chance"] <= 100 or not 0 <= values["town-chance"] <= 100:
            raise ValueError("village-chance y town-chance deben estar entre 0 y 100")
        if not 0 <= values["mob-spawn-rate"] <= 1 or not 0 <= values["structure-mob-rate"] <= 1:
            raise ValueError("las tasas de mobs deben estar entre 0 y 1")
        if values["datapack-auto-download"] and not values["datapack-url"].lower().startswith("https://"):
            raise ValueError("datapack-url debe ser una URL HTTPS")
        if not values["datapack-file-name"].endswith(".zip"):
            raise ValueError("datapack-file-name debe terminar en .zip")
        return values

    def save_config(self):
        try:
            values = self.collect_config()
            os.makedirs(os.path.dirname(self.config_path), exist_ok=True)
            backup(self.config_path)
            write_config(self.config_path, values)
            self.config_values = values
            self.log_message("Configuracion guardada.")
            return True
        except (OSError, ValueError) as error:
            messagebox.showerror("Configuracion invalida", str(error))
            return False

    def refresh_jsons(self):
        self.json_paths = json_files(self.datapack_dir)
        self.json_list.delete(0, tk.END)
        for path in self.json_paths:
            self.json_list.insert(tk.END, os.path.relpath(path, self.datapack_dir))

    def open_json(self, _event=None):
        selection = self.json_list.curselection()
        if not selection:
            return
        self.current_json = self.json_paths[selection[0]]
        try:
            with open(self.current_json, encoding="utf-8") as handle:
                content = json.dumps(json.load(handle), indent=2, ensure_ascii=False) + "\n"
            self.json_editor.delete("1.0", tk.END)
            self.json_editor.insert("1.0", content)
        except (OSError, json.JSONDecodeError) as error:
            messagebox.showerror("JSON invalido", str(error))

    def save_json(self):
        if not self.current_json:
            messagebox.showinfo("JSON", "Selecciona un archivo JSON.")
            return
        try:
            data = json.loads(self.json_editor.get("1.0", tk.END))
            backup(self.current_json)
            with open(self.current_json, "w", encoding="utf-8", newline="\n") as handle:
                json.dump(data, handle, indent=2, ensure_ascii=False)
                handle.write("\n")
            self.log_message("JSON guardado y validado: " + os.path.basename(self.current_json))
        except (OSError, json.JSONDecodeError) as error:
            messagebox.showerror("JSON invalido", str(error))

    def run_tool(self, command, label):
        try:
            process = subprocess.run(command, cwd=self.root_dir, text=True, capture_output=True)
        except OSError as error:
            messagebox.showerror(label, str(error))
            return
        output = (process.stdout + process.stderr).strip()
        self.log_message(output or label)
        if process.returncode:
            messagebox.showerror(label, output or f"El comando termino con codigo {process.returncode}")

    def generate_world(self):
        if not self.save_config():
            return
        os.makedirs(self.datapack_dir, exist_ok=True)
        climate = os.path.join(self.root_dir, "src", "main", "kotlin", "com", "example", "aeroruins", "Climate.kt")
        self.run_tool([sys.executable, os.path.join(self.root_dir, "tools", "gen_world.py"), self.datapack_dir, climate], "Error al generar biomas")
        self.refresh_jsons()

    def generate_loot(self):
        os.makedirs(self.datapack_dir, exist_ok=True)
        self.run_tool([sys.executable, os.path.join(self.root_dir, "tools", "gen_loot.py"), self.datapack_dir], "Error al generar loot")
        self.refresh_jsons()

    def package_datapack(self):
        output = os.path.join(self.root_dir, "aeroruins-datapack.zip")
        self.run_tool([sys.executable, os.path.join(self.root_dir, "tools", "package_datapack.py"), self.datapack_dir, output], "Error al empaquetar datapack")

    def build_plugin(self):
        command = "gradlew.bat" if os.name == "nt" else "./gradlew"
        self.run_tool([command, "build"], "Error al compilar")

    def log_message(self, message):
        self.log.configure(state="normal")
        self.log.insert(tk.END, message + "\n")
        self.log.see(tk.END)
        self.log.configure(state="disabled")


def main():
    parser = argparse.ArgumentParser(description="Editor interactivo de world generation de AeroRuins")
    parser.add_argument("--root", default=os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    args = parser.parse_args()
    WorldgenEditor(args.root).mainloop()


if __name__ == "__main__":
    main()
