import { API } from "./endpoints";

export async function registrationService(userData: Record<string, string>) {
    try {
      const response = await fetch(API.SIGNUP_API, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(userData),
      });
  
      const rawText = await response.clone().text(); // Non-destructive
      console.log("Raw Response Text:", rawText);
  
      if (!response.ok) {
        throw new Error(`Server Error: ${response.status} - ${rawText}`);
      }
  
      // Try to parse the raw text as JSON
      try {
        return JSON.parse(rawText);
      } catch (e) {
        // Not JSON? Return text fallback
        return { message: rawText || "Registration successful!", e };
      }
  
    } catch (error) {
      console.error("Fetch error:", error);
      throw new Error( "Network error or API issue.");
    }
  }
  