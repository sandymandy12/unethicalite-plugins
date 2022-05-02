import os
import sys
import getopt

from constants import *

def load_params(argv):
    files = ["Plugin", "Config"]
    plugin_name = ""
    description = '""'
    looped = False

    try:
        opts, argv = getopt.getopt(
            argv, "hn:f:d:l", ["name=", "files=", "description=", "looped="])
    except getopt.GetoptError:
        print('createplugin.py -n <name> -d <description> optional: -l <looped?> -f <files?>')
        sys.exit(2)

    for opt, arg in opts:
        if opt == '-h':
            print('\ncreateplugin.py -n <name> -d <description>')
            print('-f for  are comma separated. default: eg: -f "Panel,Overlay,Spell"')
            print('-l looped is optional. will use Looped extension. Otherwise Plugin extension\n')
            sys.exit()
        elif opt in ("-n", "--name"):
            plugin_name = arg
        elif opt in ("-f", "--files"):
            files += arg.split(',')
        elif opt in ("-d", "--description"):
            description = arg
        elif opt in ("-l", "--looped"):
            looped = True

    return {"name": plugin_name, "files": files, "description": description, "looped": looped}


def main(args):
    params = load_params(args)
    plugin_name = str(params['name'])
    plugin_dir_name = plugin_name.lower().replace(" ", "")

    home_dir = os.path.join(os.getcwd(), plugin_dir_name)
    plugin_src_dir = os.path.join(home_dir, INNER_PATH, plugin_dir_name)


    # <----------------------- [ plugin folder structure ] ----------------------------------------->
    print("Creating plugin --->", plugin_name, "\n")
    print("file names---> ", params["files"])

    try:
        print("building folder structure...")
        os.makedirs(plugin_src_dir)
    except FileExistsError:
        print(f'Directory {plugin_dir_name} already exists')

    # <----------------------- [ creating gradle file] ----------------------------------------->
    print("creating gradle file...")
    gradle_template = parse(PLUGIN_GRADLE_TEMPLATE,
                            plugin_name, params["description"])

    gradle_file_path = os.path.join(home_dir, f'{plugin_dir_name}.gradle.kts')

    print("creating gradle.kts file...")
    gradle_file = open(gradle_file_path, "w")
    gradle_file.write(gradle_template)
    gradle_file.close()

    # <----------------------- [ create plugin files] ----------------------------------------->
    print("generating plugin files...")
    # key words to parse: UPPERCASED, LOWERCASED, CAPITALIZED, FULLNAME

    plugin_template = LOOPED_TEMPLATE if params["looped"] else CLASSIC_TEMPLATE

    for i in params["files"]:
        name_cap = plugin_name.replace(" ", "")
        file_path = os.path.join(plugin_src_dir, f'{name_cap}{i}.java')
        file = open(file_path, "w")

        parsed_template = ""

        if str(i).lower() == "plugin":
            parsed_template = parse(plugin_template, plugin_name, params['description'])
        elif str(i).lower() == "panel":
            parsed_template = parse(PANEL_TEMPLATE, plugin_name)
        elif str(i).lower() == "config":
            parsed_template = parse(CONFIG_TEMPLATE, plugin_name)
        else:
            parsed_template = f'package dev.unethicalite.{plugin_name};\n'

        file.write(parsed_template)
        file.close()

    # <----------------------- [ append build.gradle ] ----------------------------------------->
    print("appending gradle settings...")
    settings_file = os.path.join(os.getcwd(), "settings.gradle.kts")

    lowercased = plugin_name.lower().replace(" ", "")
    setting_input = f'include("{lowercased}")'

    f = open(settings_file, "r")
    file_data = f.read()
    f.close()

    if setting_input not in file_data:
        print("adding..")
        for line in file_data.splitlines():
            if line.startswith("include"):
                new_line = setting_input + "\n" + line
                file_data = file_data.replace(line, new_line)
                with open(settings_file, "w") as f:
                    f.write(file_data)
                break
    else:
        print(lowercased, "is already added gradle settings..")


def parse(template, name, description=""):
    temp = str(template)
    temp = temp.replace(
        "UPPERCASED", name.upper().replace(" ", ""))
    temp = temp.replace(
        "LOWERCASED", name.lower().replace(" ", ""))
    temp = temp.replace(
        "CAPITALIZED", name.replace(" ", ""))
    temp = temp.replace("FULLNAME", name)
    temp = temp.replace("DESCRIPTION", description)

    return temp


if __name__ == "__main__":
    main(sys.argv[1:])
