import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { useState, ChangeEvent, useEffect } from "react";
import { Button } from "@/components/ui/button";
import StarRating from "./StarRating";
import { FaUserCircle } from "react-icons/fa";
import { DialogDescription } from "@radix-ui/react-dialog";
import { FeedbackData, Reservation } from "@/types/FormData";
import { FaStar } from "react-icons/fa";
import { API } from "@/api/endpoints";

interface FeedbackModalProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (feedback: FeedbackData) => void;
  reservation: Reservation;
}

const FeedbackModal: React.FC<FeedbackModalProps> = ({
  open,
  onClose,
  onSubmit,
  reservation,
}) => {
  const [selectedTab, setSelectedTab] = useState<"service" | "culinary">(
    "service"
  );
  const [feedback, setFeedback] = useState<FeedbackData>({
    serviceRating: 0,
    serviceComment: "",
    cuisineRating: 0,
    cuisineComment: "",
  });

  const handleTabChange = (tab: "service" | "culinary") => {
    setSelectedTab(tab);
  };

  const handleRatingChange = (
    name: "serviceRating" | "cuisineRating",
    rating: number
  ) => {
    setFeedback((prevFeedback) => ({
      ...prevFeedback,
      [name]: rating,
    }));
  };

  const handleCommentsChange = (e: ChangeEvent<HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setFeedback((prevFeedback) => ({
      ...prevFeedback,
      [name]: value,
    }));
  };

  const handleSubmit = () => {
    onSubmit(feedback);
    setFeedback({
      serviceRating: 0,
      serviceComment: "",
      cuisineRating: 0,
      cuisineComment: "",
    });
    onClose();
  };
  useEffect(() => {
    const getFeedback = async () => {
      try {
        const response = await fetch(
          `${API.FEEDBACKS_POST}/${reservation.id}`,
          {
            method: "GET",
            headers: {
              "Content-Type": "application/json",
              Authorization: `Bearer ${sessionStorage.getItem("token")}`,
            },
          }
        );
        const responseData = await response.json();
        setFeedback({
          serviceRating: responseData.serviceRating || 0,
          serviceComment: responseData.serviceComment || "",
          cuisineRating: responseData.cuisineRating || 0,
          cuisineComment: responseData.cuisineComment || "",
        });
      } catch (error) {
        console.error(error);
      }
    };
    getFeedback();
  }, [open]);

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle className="font-semibold">Give Feedback</DialogTitle>
          <DialogDescription className="font-extralight">
            Please rate your experience below
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          <div className="flex space-x-4">
            <button
              className={`flex-1 py-2 ${
                selectedTab === "service"
                  ? "border-b-4 border-green-500 font-bold text-green-500"
                  : "border-b-4 border-transparent"
              }`}
              onClick={() => handleTabChange("service")}
            >
              Service
            </button>
            <button
              className={`flex-1 py-2 ${
                selectedTab === "culinary"
                  ? "border-b-4 border-green-500 font-bold text-green-500"
                  : "border-b-4 border-transparent"
              }`}
              onClick={() => handleTabChange("culinary")}
            >
              Culinary Experience
            </button>
          </div>

          {selectedTab === "service" && (
            <div className="space-y-4">
              <div className="flex flex-col items-start space-y-1">
                <div className="flex items-center space-x-2">
                  <FaUserCircle size={24} className="text-gray-500" />
                  <strong>{reservation?.waiterEmail}</strong>
                </div>
                <div className="flex items-center">
                  <span>4.6</span>
                  <FaStar className="text-yellow-500" />
                </div>
              </div>
              <div className="flex items-center space-x-2">
                <StarRating
                  rating={feedback.serviceRating}
                  setRating={(rating) =>
                    handleRatingChange("serviceRating", rating)
                  }
                />
                <span>{feedback.serviceRating}/5 stars</span>
              </div>
              <div>
                <textarea
                  id="serviceComment"
                  name="serviceComment"
                  rows={3}
                  className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                  value={feedback.serviceComment}
                  onChange={handleCommentsChange}
                  placeholder="Add your comment"
                />
              </div>
            </div>
          )}

          {selectedTab === "culinary" && (
            <div className="space-y-4">
              <div className="flex items-center space-x-2">
                <StarRating
                  rating={feedback.cuisineRating}
                  setRating={(rating) =>
                    handleRatingChange("cuisineRating", rating)
                  }
                />
                <span>{feedback.cuisineRating}/5 stars</span>
              </div>
              <div>
                <textarea
                  id="cuisineComment"
                  name="cuisineComment"
                  rows={3}
                  className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                  value={feedback.cuisineComment}
                  onChange={handleCommentsChange}
                  placeholder="Add your comment"
                />
              </div>
            </div>
          )}

          <div className="flex justify-end">
            <Button
              onClick={handleSubmit}
              className="bg-green-500 text-white px-4 py-2 rounded-md"
            >
              Submit Feedback
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
};

export default FeedbackModal;
