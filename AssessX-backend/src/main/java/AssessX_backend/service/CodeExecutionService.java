package AssessX_backend.service;

import AssessX_backend.dto.CodeSubmissionResultDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class CodeExecutionService {

    @Value("${app.sandbox.volume:assessx_sandbox}")
    private String sandboxVolume;

    @Value("${app.sandbox.path:/sandbox}")
    private String sandboxPath;

    public CodeSubmissionResultDto execute(String studentCode, List<String> unitTestCodes, int timeLimitSec) {
        int total = unitTestCodes.size();
        if (total == 0) {
            return new CodeSubmissionResultDto(0, 0, "No unit tests defined");
        }

        String taskId = UUID.randomUUID().toString();
        Path taskDir = Paths.get(sandboxPath, taskId);

        try {
            Files.createDirectories(taskDir);
            System.out.println("[Sandbox] Created dir: " + taskDir);

            Files.writeString(taskDir.resolve("Solution.java"), studentCode);
            Files.writeString(taskDir.resolve("Runner.java"), buildRunner(unitTestCodes));
            System.out.println("[Sandbox] Files written");

            String[] cmd = {
                "docker", "run", "--rm",
                "--network", "none",
                "--memory", "128m",
                "--cpus", "0.5",
                "eclipse-temurin:21-jdk-alpine",
                "sh", "-c",
                "mkdir -p /tmp/task && " +
                "cat > /tmp/task/Solution.java << 'JAVAEOF'\n" + studentCode + "\nJAVAEOF\n" +
                "cat > /tmp/task/Runner.java << 'JAVAEOF'\n" + buildRunner(unitTestCodes) + "\nJAVAEOF\n" +
                "cd /tmp/task && javac Solution.java Runner.java 2>&1 && java -ea Runner 2>&1"
            };

            System.out.println("[Sandbox] Running docker command");
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(timeLimitSec + 10L, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                deleteDir(taskDir.toFile());
                return new CodeSubmissionResultDto(0, total, "Execution timed out");
            }

            String outputStr = output.toString();
            deleteDir(taskDir.toFile());
            return new CodeSubmissionResultDto(parsePassedCount(outputStr), total, outputStr);

        } catch (Exception e) {
            System.err.println("[Sandbox] Error: " + e.getMessage());
            e.printStackTrace();
            
            deleteDir(taskDir.toFile());
            return new CodeSubmissionResultDto(0, total, "Execution error: " + e.getMessage());
        }
    }

    private String buildRunner(List<String> unitTestCodes) {
        StringBuilder sb = new StringBuilder();
        sb.append("public class Runner {\n");
        sb.append("    public static void main(String[] args) {\n");
        sb.append("        int passed = 0, total = ").append(unitTestCodes.size()).append(";\n");
        for (int i = 0; i < unitTestCodes.size(); i++) {
            sb.append("        // Test ").append(i + 1).append("\n");
            sb.append("        try {\n");
            for (String line : unitTestCodes.get(i).split("\n")) {
                sb.append("            ").append(line).append("\n");
            }
            sb.append("            passed++;\n");
            sb.append("        } catch (Throwable t) {\n");
            sb.append("            System.out.println(\"Test ").append(i + 1)
              .append(" FAILED: \" + t.getMessage());\n");
            sb.append("        }\n");
        }
        sb.append("        System.out.println(\"RESULT:\" + passed + \"/\" + total);\n");
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }

    private int parsePassedCount(String output) {
        for (String line : output.split("\n")) {
            if (line.startsWith("RESULT:")) {
                try {
                    return Integer.parseInt(line.substring(7).split("/")[0].trim());
                } catch (Exception ignored) {}
            }
        }
        return 0;
    }

    private void deleteDir(java.io.File dir) {
        if (dir == null || !dir.exists()) return;
        java.io.File[] files = dir.listFiles();
        if (files != null) {
            for (java.io.File f : files) {
                if (f.isDirectory()) deleteDir(f);
                else f.delete();
            }
        }
        dir.delete();
    }
}