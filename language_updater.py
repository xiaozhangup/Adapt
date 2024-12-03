import json
import tkinter as tk
from tkinter import filedialog, messagebox

# 加载 JSON 文件
def load_json(file_path):
    with open(file_path, 'r', encoding='utf-8') as file:
        return json.load(file)

# 保存 JSON 文件
def save_json(data, file_path):
    with open(file_path, 'w', encoding='utf-8') as file:
        json.dump(data, file, ensure_ascii=False, indent=4)

# 合并语言文件
def merge_language_files(en_file, zh_file, output_file):
    en_data = load_json(en_file)
    zh_data = load_json(zh_file)

    def update_dict(en_dict, zh_dict):
        for key, value in en_dict.items():
            if key not in zh_dict:
                zh_dict[key] = value
            elif isinstance(value, dict):
                if key not in zh_dict or not isinstance(zh_dict[key], dict):
                    zh_dict[key] = {}
                update_dict(value, zh_dict[key])

    update_dict(en_data, zh_data)
    save_json(zh_data, output_file)

# 界面功能
def select_en_file():
    file_path = filedialog.askopenfilename(filetypes=[("JSON files", "*.json")])
    if file_path:
        en_path.set(file_path)

def select_zh_file():
    file_path = filedialog.askopenfilename(filetypes=[("JSON files", "*.json")])
    if file_path:
        zh_path.set(file_path)

def generate_output():
    en_file = en_path.get()
    zh_file = zh_path.get()
    if not en_file or not zh_file:
        messagebox.showerror("错误", "请选择 en_US.json 和 zh_CN.json 文件！")
        return
    
    output_file = filedialog.asksaveasfilename(defaultextension=".json", filetypes=[("JSON files", "*.json")])
    if output_file:
        try:
            merge_language_files(en_file, zh_file, output_file)
            messagebox.showinfo("成功", f"更新完成！结果已保存到：\n{output_file}")
        except Exception as e:
            messagebox.showerror("错误", f"处理文件时出错：\n{e}")

# 创建主窗口
root = tk.Tk()
root.title("语言文件更新工具")

# 文件路径变量
en_path = tk.StringVar()
zh_path = tk.StringVar()

# 布局
frame = tk.Frame(root, padx=10, pady=10)
frame.pack(fill="both", expand=True)

tk.Label(frame, text="选择 en_US.json 文件：").grid(row=0, column=0, sticky="w")
tk.Entry(frame, textvariable=en_path, width=50).grid(row=0, column=1, padx=5)
tk.Button(frame, text="浏览", command=select_en_file).grid(row=0, column=2, padx=5)

tk.Label(frame, text="选择 zh_CN.json 文件：").grid(row=1, column=0, sticky="w")
tk.Entry(frame, textvariable=zh_path, width=50).grid(row=1, column=1, padx=5)
tk.Button(frame, text="浏览", command=select_zh_file).grid(row=1, column=2, padx=5)

tk.Button(frame, text="生成更新文件", command=generate_output).grid(row=2, column=0, columnspan=3, pady=10)

# 启动主循环
root.mainloop()
