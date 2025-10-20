import { toast } from "react-toastify";
import { API } from "./endpoints";

interface formType {
    email: string;
    password: string;
  }

export async function loginService(userData: formType) {
  try {
    const response = await fetch(API.LOGIN_API, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(userData),
    });
    if (!response.ok) {
      const responseData = await response.json();
      console.log(responseData.message)
      toast.error( responseData.message)
      throw new Error(`Server Error: ${response.status} - ${response.text()}`);
    }
    return await response.json();
  } catch (error) {
    console.error("Fetch error: ", error);
    throw new Error("Network error or API issue.");
  }
}
