rule:
    name:
        module_name
    input:
        data=alg_input_data()
    output:
        adjmat=alg_output_adjmat_path(module_name),
        time=alg_output_time_path(module_name),
        ntests=alg_output_ntests_path(module_name)
    container:
        "docker://bpimages/causaldag:0.1a163-arm64"
    script:
        "script.py"