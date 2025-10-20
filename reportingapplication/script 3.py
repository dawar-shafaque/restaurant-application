import os

def merge_java_files(src_dir, output_file):
    with open(output_file, 'w') as outfile:
        for root, _, files in os.walk(src_dir):
            for file in files:
                if file.endswith(".java"):
                    file_path = os.path.join(root, file)
                    with open(file_path, 'r') as infile:
                        outfile.write(infile.read())
                        outfile.write("\n")  # Add a newline to separate files

if __name__ == "__main__":
    src_dir = "C:/Users/dawar_shafaque/MyStuffs/final PBE/reportingapplication/src"
    output_file = "merged_output.java"
    merge_java_files(src_dir, output_file)
    print(f"All Java files have been merged into {output_file}")
