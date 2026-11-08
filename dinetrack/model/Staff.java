package dinetrack.model;

public class Staff extends User {
    private String jobTitle;

    public Staff(int id, String name, String phone, String email, String jobTitle) {
        super(id, name, phone, email);
        setJobTitle(jobTitle);
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        if (jobTitle == null || jobTitle.trim().isEmpty()) {
            throw new IllegalArgumentException("Job title cannot be empty.");
        }
        this.jobTitle = jobTitle.trim();
    }

    @Override
    public String getRole() {
        return "Staff (" + jobTitle + ")";
    }
}

