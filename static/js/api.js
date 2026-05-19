// ========================================
// FitJournal API JavaScript
// Handles all frontend-backend communication
// ========================================

// API base URL - change this when you deploy
const API_URL = 'http://127.0.0.1:8000';

// ========================================
// AUTHENTICATION
// ========================================

async function registerUser(email, password) {
    try {
        const response = await fetch(`${API_URL}/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                user_email: email,
                user_password: password
            })
        });

        const data = await response.json();

        if (response.ok) {
            return { success: true };
        } else {
            return {
                success: false,
                error: data.detail || 'Registration failed. Please try again.'
            };
        }
    } catch (error) {
        console.error('Register error:', error);
        return {
            success: false,
            error: 'Network error. Please check if the backend is running.'
        };
    }
}

async function loginUser(email, password) {
    try {
        const response = await fetch(`${API_URL}/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                user_email: email,
                user_password: password
            })
        });

        const data = await response.json();

        if (response.ok) {
            localStorage.setItem('user_id', data.user_id);
            localStorage.setItem('user_email', data.user_email);
            localStorage.setItem('access_token', data.access_token);

            // Fetch first name in the background (non-blocking)
            try {
                const profileRes = await fetch(`${API_URL}/profile/${data.user_id}`, {
                    headers: { 'Authorization': `Bearer ${data.access_token}` }
                });
                if (profileRes.ok) {
                    const profile = await profileRes.json();
                    if (profile && profile.user_first_name) {
                        localStorage.setItem('user_first_name', profile.user_first_name);
                    }
                }
            } catch (e) {
                // Profile fetch failure is non-fatal — login still succeeds
                console.warn('Could not fetch user profile after login:', e);
            }

            return { success: true, user_id: data.user_id };
        } else {
            return {
                success: false,
                error: data.detail || 'Invalid email or password.'
            };
        }
    } catch (error) {
        console.error('Login error:', error);
        return {
            success: false,
            error: 'Network error. Please check if the backend is running.'
        };
    }
}

// ========================================
// HELPER FUNCTIONS
// ========================================

// Check if user is logged in
function isLoggedIn() {
    return localStorage.getItem('access_token') !== null;
}

// Get current user ID
function getCurrentUserId() {
    return localStorage.getItem('user_id');
}

// Get current user email
function getCurrentUserEmail() {
    return localStorage.getItem('user_email');
}

// Get current user first name
function getCurrentUserFirstName() {
    return localStorage.getItem('user_first_name');
}

// Get stored JWT token
function getToken() {
    return localStorage.getItem('access_token');
}

// Build Authorization header for API calls
function authHeaders() {
    const token = getToken();
    return {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : ''
    };
}

// Logout function
function logout() {
    localStorage.removeItem('user_id');
    localStorage.removeItem('user_email');
    localStorage.removeItem('user_first_name');
    localStorage.removeItem('access_token');
    window.location.href = '/web/login';
}

// Protect pages that require login
function requireLogin() {
    if (!isLoggedIn()) {
        alert('Please log in to access this page');
        window.location.href = '/web/login';
    }
}

// ========================================
// EXERCISES (to be used in /exercises)
// ========================================

// Get all exercises for current user
async function getUserExercises() {
    const userId = getCurrentUserId();
    
    if (!userId) {
        alert('Please log in first');
        window.location.href = '/web/login';
        return;
    }
    
    try {
        const response = await fetch(`${API_URL}/exercises?user_id=${userId}`, {
            headers: authHeaders()
        });
        
        if (response.ok) {
            const exercises = await response.json();
            return exercises;
        } else {
            console.error('Failed to fetch exercises');
            return [];
        }
    } catch (error) {
        console.error('Error fetching exercises:', error);
        return [];
    }
}

// Create new exercise
async function createExercise(exerciseData) {
    const userId = getCurrentUserId();
    
    if (!userId) {
        alert('Please log in first');
        window.location.href = '/web/login';
        return;
    }
    
    try {
        const response = await fetch(`${API_URL}/exercises?user_id=${userId}`, {
            method: 'POST',
            headers: authHeaders(),
            body: JSON.stringify(exerciseData)
        });
        
        if (response.ok) {
            const newExercise = await response.json();
            return newExercise;
        } else {
            const error = await response.json();
            alert(`Error creating exercise: ${error.detail}`);
            return null;
        }
    } catch (error) {
        console.error('Error creating exercise:', error);
        alert('Network error. Please try again.');
        return null;
    }
}

// Delete exercise
async function deleteExercise(exerciseId) {
    const userId = getCurrentUserId();
    
    if (!userId) {
        alert('Please log in first');
        window.location.href = '/web/login';
        return;
    }
    
    try {
        const response = await fetch(`${API_URL}/exercises/${exerciseId}?user_id=${userId}`, {
            method: 'DELETE',
            headers: authHeaders()
        });
        
        if (response.ok) {
            return true;
        } else {
            const error = await response.json();
            alert(`Error deleting exercise: ${error.detail}`);
            return false;
        }
    } catch (error) {
        console.error('Error deleting exercise:', error);
        alert('Network error. Please try again.');
        return false;
    }
}

// ========================================
// PROFILE
// ========================================

// Get user profile
async function getUserProfile() {
    const userId = getCurrentUserId();
    
    if (!userId) {
        alert('Please log in first');
        window.location.href = '/web/login';
        return;
    }
    
    try {
        const response = await fetch(`${API_URL}/profile/${userId}`, {
            headers: authHeaders()
        });
        
        if (response.ok) {
            const profile = await response.json();
            return profile;
        } else {
            console.error('Failed to fetch profile');
            return null;
        }
    } catch (error) {
        console.error('Error fetching profile:', error);
        return null;
    }
}

// Update user profile
async function updateUserProfile(profileData) {
    const userId = getCurrentUserId();
    
    if (!userId) {
        alert('Please log in first');
        window.location.href = '/web/login';
        return;
    }
    
    try {
        const response = await fetch(`${API_URL}/profile/${userId}`, {
            method: 'PUT',
            headers: authHeaders(),
            body: JSON.stringify(profileData)
        });
        
        if (response.ok) {
            const updatedProfile = await response.json();
            return updatedProfile;
        } else {
            const error = await response.json();
            alert(`Error updating profile: ${error.detail}`);
            return null;
        }
    } catch (error) {
        console.error('Error updating profile:', error);
        alert('Network error. Please try again.');
        return null;
    }
}


// ========================================
// ROUTINE
// ========================================

// Get user routine
async function getUserRoutine() {
    const userId = getCurrentUserId();
    
    if (!userId) {
        alert('Please log in first');
        window.location.href = '/web/login';
        return;
    }
    
    try {
        const response = await fetch(`${API_URL}/routine/${userId}`, {
            headers: authHeaders()
        });
        
        if (response.ok) {
            const routine = await response.json();
            return routine;
        } else {
            console.error('Failed to fetch routine');
            return null;
        }
    } catch (error) {
        console.error('Error fetching routine:', error);
        return null;
    }
}

// Save user routine
async function saveUserRoutine(routineData) {
    const userId = getCurrentUserId();
    
    if (!userId) {
        alert('Please log in first');
        window.location.href = '/web/login';
        return;
    }
    
    try {
        const response = await fetch(`${API_URL}/routine/${userId}`, {
            method: 'POST',
            headers: authHeaders(),
            body: JSON.stringify(routineData)
        });
        
        if (response.ok) {
            const result = await response.json();
            return result;
        } else {
            const error = await response.json();
            alert(`Error saving routine: ${error.detail}`);
            return null;
        }
    } catch (error) {
        console.error('Error saving routine:', error);
        alert('Network error. Please try again.');
        return null;
    }
}

// Delete user routine
async function deleteUserRoutine() {
    const userId = getCurrentUserId();
    
    if (!userId) {
        alert('Please log in first');
        window.location.href = '/web/login';
        return;
    }
    
    try {
        const response = await fetch(`${API_URL}/routine/${userId}`, {
            method: 'DELETE',
            headers: authHeaders()
        });
        
        if (response.ok) {
            return true;
        } else {
            console.error('Failed to delete routine');
            return false;
        }
    } catch (error) {
        console.error('Error deleting routine:', error);
        return false;
    }
}


// ========================================
// CALENDAR & NEXT WORKOUT
// ========================================

// Get workout logs
async function getWorkoutLogs(userId, limit = 30) {
    try {
        const response = await fetch(`${API_URL}/workout/logs/${userId}?limit=${limit}`, {
            headers: authHeaders()
        });
        if (response.ok) {
            return await response.json();
        }
        return [];
    } catch (error) {
        console.error('Error fetching workout logs:', error);
        return [];
    }
}

// Get next workout selections
async function getNextWorkoutSelections(userId) {
    try {
        const response = await fetch(`${API_URL}/next-workout/selections/${userId}`, {
            headers: authHeaders()
        });
        if (response.ok) {
            const data = await response.json();
            // Convert array to object for easier lookup
            const selections = {};
            data.forEach(item => {
                selections[item.exercise_id] = item.is_selected;
            });
            return selections;
        }
        return {};
    } catch (error) {
        console.error('Error fetching next workout selections:', error);
        return {};
    }
}

// Get workout sessions (last N sessions)
async function getWorkoutSessions(userId, limit = 10) {
    try {
        const response = await fetch(`${API_URL}/workout/sessions/${userId}?limit=${limit}`, {
            headers: authHeaders()
        });
        if (response.ok) {
            return await response.json();
        }
        return [];
    } catch (error) {
        console.error('Error fetching workout sessions:', error);
        return [];
    }
}

// Get workout logs by session IDs
async function getWorkoutLogsBySessions(userId, sessionIds) {
    try {
        const response = await fetch(`${API_URL}/workout/logs-by-sessions/${userId}`, {
            method: 'POST',
            headers: authHeaders(),
            body: JSON.stringify({ session_ids: sessionIds })
        });
        if (response.ok) {
            return await response.json();
        }
        return [];
    } catch (error) {
        console.error('Error fetching workout logs:', error);
        return [];
    }
}


// ========================================
// CONSOLE LOG (for debugging)
// ========================================

console.log('FitJournal API loaded successfully');
console.log('API URL:', API_URL);
if (isLoggedIn()) {
    console.log('User logged in:', getCurrentUserEmail());
} else {
    console.log('No user logged in');
}