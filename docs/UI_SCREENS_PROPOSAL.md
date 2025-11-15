# Tresor - UI Screens Proposal

## Overview
Tresor is a time-locked message application that allows users to send encrypted messages to their future selves or others, with guaranteed delivery at a specific future date using blockchain technology.

---

## 1. Onboarding / Landing Screen

### Purpose
First impression - explain the core value proposition and build trust.

### Elements
- **Hero Section**
  - Large headline: "Messages That Wait for You"
  - Subheadline: "Send encrypted messages to your future self. Guaranteed delivery using blockchain technology."
  - Hero image/animation: A locked chest with a timer, or calendar morphing into an envelope

- **Trust Indicators**
  - "🔒 Military-grade encryption"
  - "⛓️ Blockchain-secured"
  - "📅 Guaranteed time-lock"
  - "💰 Pay-once, no subscriptions"

- **CTA Buttons**
  - Primary: "Create Your First Message" (large, prominent)
  - Secondary: "Learn How It Works"

- **Use Cases Carousel**
  - "Write to your future self"
  - "Birthday surprises for loved ones"
  - "Time capsules for special moments"
  - "Deferred business communications"

### Figma Design Tips
- Use a gradient background (deep blue to purple) to convey trust and technology
- Large, clear typography (60-80px for headline)
- Animated hero element using Figma's Smart Animate for prototype
- Min-height: 100vh for full-screen impact
- Desktop: 1440px wide, Mobile: 375px wide

---

## 2. How It Works Screen

### Purpose
Educate users on the process and build confidence in the technology.

### Elements
- **3-Step Visual Flow**

  **Step 1: Write & Lock**
  - Icon: ✍️ Pencil writing
  - "Write your message and choose your unlock date"
  - Visual: Text editor with calendar picker

  **Step 2: Encrypt & Deploy**
  - Icon: 🔐 Lock with blockchain nodes
  - "Your message is encrypted and distributed across blockchains"
  - Visual: Message being split into fragments going to different chains

  **Step 3: Automatic Unlock**
  - Icon: 📬 Unlocking mailbox
  - "On your chosen date, your message is automatically delivered"
  - Visual: Calendar turning to target date, message appearing in inbox

- **Technical Trust Section**
  - "Shamir Secret Sharing" explanation with simple diagram
  - "No one can access your message before the unlock date - not even us"
  - Link to "View Technical Details"

### Figma Design Tips
- Use a horizontal timeline for desktop, vertical for mobile
- Isometric illustrations for each step
- Subtle animations showing the flow between steps
- Color coding: Step 1 (blue), Step 2 (purple), Step 3 (green)
- Use Auto Layout for responsive design

---

## 3. Dashboard / Home Screen

### Purpose
Central hub showing all messages (sent, pending, received).

### Layout
```
┌─────────────────────────────────────────────────┐
│  Tresor                          [Profile] [⚙️]  │
├─────────────────────────────────────────────────┤
│                                                  │
│  Welcome back, [Name]! 👋                       │
│                                                  │
│  ┌─────────────────────────────────────────┐   │
│  │  📊 Your Messages at a Glance            │   │
│  │  • 5 Locked Messages                     │   │
│  │  • 2 Unlocking This Week                │   │
│  │  • 12 Messages Delivered                │   │
│  └─────────────────────────────────────────┘   │
│                                                  │
│  [+ Create New Message]  (large primary button) │
│                                                  │
│  Tabs: [Pending] [Upcoming] [Delivered] [All]  │
│                                                  │
│  ┌─────────────────────────────────────────┐   │
│  │ 🎂 Birthday Message for Sarah            │   │
│  │ Unlocks: Dec 15, 2024 (in 28 days)      │   │
│  │ ⏰ Tier: Standard                         │   │
│  │ Status: [■■■■■■░░░░] Deploying (60%)     │   │
│  └─────────────────────────────────────────┘   │
│                                                  │
│  ┌─────────────────────────────────────────┐   │
│  │ 💭 Dear Future Me - 2025 Goals           │   │
│  │ Unlocks: Jan 1, 2025 (in 47 days)       │   │
│  │ 💰 Tier: Budget                           │   │
│  │ Status: Pending Batch Deployment         │   │
│  └─────────────────────────────────────────┘   │
│                                                  │
└─────────────────────────────────────────────────┘
```

### Elements
- **Header**
  - Tresor logo (left)
  - Search bar (center)
  - Profile avatar + Settings icon (right)

- **Stats Overview Card**
  - Quick metrics in card format
  - Subtle gradient background

- **Primary CTA**
  - "Create New Message" button - large, always visible
  - Should stand out with vibrant color

- **Tab Navigation**
  - Pending: Messages waiting for deployment
  - Upcoming: Messages deploying soon
  - Delivered: Successfully unlocked messages
  - All: Complete archive

- **Message Cards**
  - Title/subject of message
  - Unlock date with countdown
  - Deployment tier badge (Budget/Standard/Premium/Enterprise)
  - Status indicator with progress bar
  - Hover: Quick actions (View Details, Cancel)

### Figma Design Tips
- Use a card-based layout with 16px padding
- Message cards: 8px border-radius, subtle shadow (0px 2px 8px rgba(0,0,0,0.1))
- Color-code status: Orange (pending), Blue (deploying), Green (delivered)
- Add hover states showing quick actions
- Use Components for message cards (make them reusable)
- Desktop: 2 columns, Tablet: 1 column, Mobile: Stack vertically

---

## 4. Create Message Screen

### Purpose
Main interface for composing a new time-locked message.

### Layout
```
┌─────────────────────────────────────────────────┐
│  ← Back          Create New Message         [?] │
├─────────────────────────────────────────────────┤
│                                                  │
│  Step 1 of 4: Compose Your Message              │
│  [█████████░░░░░░░░░░░░░░░░] 30%                │
│                                                  │
│  ┌──────────────────────────────────────────┐  │
│  │ To: [Recipient]                           │  │
│  │ ○ Myself   ○ Someone Else                │  │
│  └──────────────────────────────────────────┘  │
│                                                  │
│  ┌──────────────────────────────────────────┐  │
│  │ Subject (optional)                        │  │
│  │ [                                    ]    │  │
│  └──────────────────────────────────────────┘  │
│                                                  │
│  ┌──────────────────────────────────────────┐  │
│  │ Your Message                              │  │
│  │ ┌────────────────────────────────────┐   │  │
│  │ │ Write your message here...          │   │  │
│  │ │                                     │   │  │
│  │ │                                     │   │  │
│  │ │ [B] [I] [📎] [😊]                   │   │  │
│  │ └────────────────────────────────────┘   │  │
│  └──────────────────────────────────────────┘  │
│                                                  │
│  ┌──────────────────────────────────────────┐  │
│  │ 📎 Attachments (optional)                │  │
│  │ [+ Add Files]                             │  │
│  │ Supported: Images, PDFs, Audio            │  │
│  └──────────────────────────────────────────┘  │
│                                                  │
│  [Cancel]                    [Next: Set Date →] │
│                                                  │
└─────────────────────────────────────────────────┘
```

### Step-by-Step Flow

**Step 1: Compose Message**
- Recipient selection (self or email address)
- Optional subject line
- Rich text editor for message body
- Attachment uploader (drag & drop)
- Character count / file size indicators

**Step 2: Set Unlock Date**
```
┌──────────────────────────────────────────┐
│  When should this message unlock?        │
│                                           │
│  📅 [Date Picker]                        │
│      [December 25, 2024]                 │
│                                           │
│  🕐 [Time Picker]                        │
│      [12:00 PM] [Timezone: PST]          │
│                                           │
│  Quick Presets:                          │
│  [1 Month] [6 Months] [1 Year] [5 Years]│
│                                           │
│  ⏰ Message will unlock in:              │
│     182 days, 5 hours from now           │
│                                           │
└──────────────────────────────────────────┘
```

**Step 3: Choose Security & Deployment Tier**
```
┌──────────────────────────────────────────┐
│  Select Deployment Tier                  │
│                                           │
│  ┌─────────────────────────────────────┐│
│  │ 💰 BUDGET - $0.50                   ││
│  │ • Batch deployment (daily)          ││
│  │ • Password encryption               ││
│  │ • Email delivery                    ││
│  │ Best for: Personal notes            ││
│  │ [Select]                            ││
│  └─────────────────────────────────────┘│
│                                           │
│  ┌─────────────────────────────────────┐│
│  │ ⭐ STANDARD - $3.00    [RECOMMENDED]││
│  │ • Immediate deployment              ││
│  │ • 3-of-5 blockchain distribution    ││
│  │ • Full encryption                   ││
│  │ Best for: Important messages        ││
│  │ [Select]                            ││
│  └─────────────────────────────────────┘│
│                                           │
│  ┌─────────────────────────────────────┐│
│  │ 💎 PREMIUM - $13.00                 ││
│  │ • 5-of-7 blockchain distribution    ││
│  │ • Maximum redundancy                ││
│  │ • Priority support                  ││
│  │ Best for: Critical communications   ││
│  │ [Select]                            ││
│  └─────────────────────────────────────┘│
│                                           │
│  [Compare Tiers]                         │
└──────────────────────────────────────────┘
```

**Step 4: Security & Confirmation**
```
┌──────────────────────────────────────────┐
│  Secure Your Message                     │
│                                           │
│  Create a password (if Budget tier):     │
│  ┌─────────────────────────────────────┐│
│  │ Password: [••••••••••]              ││
│  │ Strength: [████████░░] Strong       ││
│  └─────────────────────────────────────┘│
│                                           │
│  ┌─────────────────────────────────────┐│
│  │ Password Hint (optional):           ││
│  │ [My first pet's name]               ││
│  └─────────────────────────────────────┘│
│                                           │
│  ⚠️ Important: Save your password!      │
│     We cannot recover it for you.        │
│                                           │
│  ─────────────────────────────────────   │
│                                           │
│  Review & Confirm:                       │
│  • To: Myself (your@email.com)          │
│  • Unlock: Dec 25, 2024 at 12:00 PM     │
│  • Tier: Standard ($3.00)               │
│  • Message size: 1.2 KB                 │
│                                           │
│  [✓] I understand my message cannot be  │
│      accessed until the unlock date      │
│                                           │
│  [Back]              [Create & Pay $3 →] │
│                                           │
└──────────────────────────────────────────┘
```

### Figma Design Tips
- Create a Component Set for the 4 steps
- Use consistent spacing (8px grid system)
- Progress bar should be animated
- Tier cards: Use distinct colors (Budget: blue, Standard: purple, Premium: gold)
- Add micro-interactions: Button hover states, input focus states
- Mobile: Stack tier cards vertically, reduce padding to 16px
- Use "sticky" navigation buttons at bottom for mobile

---

## 5. Payment Screen

### Purpose
Secure payment processing with clear pricing breakdown.

### Layout
```
┌─────────────────────────────────────────────────┐
│  🔒 Secure Payment                              │
├─────────────────────────────────────────────────┤
│                                                  │
│  Order Summary                                  │
│  ┌──────────────────────────────────────────┐  │
│  │ Standard Deployment                $3.00  │  │
│  │ ─────────────────────────────────────    │  │
│  │ • 3-of-5 blockchain distribution          │  │
│  │ • Immediate deployment                    │  │
│  │ • Full encryption                         │  │
│  │ • Email notification                      │  │
│  │                                           │  │
│  │ Total:                            $3.00  │  │
│  └──────────────────────────────────────────┘  │
│                                                  │
│  Payment Method                                 │
│  ┌──────────────────────────────────────────┐  │
│  │ [💳] Credit Card                         │  │
│  │                                           │  │
│  │ Card Number                               │  │
│  │ [4242 4242 4242 4242]                    │  │
│  │                                           │  │
│  │ Expiry        CVV                         │  │
│  │ [12/25]      [123]                       │  │
│  │                                           │  │
│  │ Cardholder Name                           │  │
│  │ [John Doe]                               │  │
│  └──────────────────────────────────────────┘  │
│                                                  │
│  🔒 Secured by Stripe                           │
│                                                  │
│  [Cancel]            [Complete Payment $3.00 →] │
│                                                  │
└─────────────────────────────────────────────────┘
```

### Elements
- **Order Summary Card**
  - Tier name and price (large, bold)
  - Feature breakdown
  - Total with tax breakdown if applicable

- **Payment Form**
  - Stripe Elements integration (not designed in Figma, just placeholder)
  - Trust badges (SSL, Stripe)
  - Terms & conditions checkbox

- **Success State**
  - Checkmark animation
  - "Your message is being deployed!"
  - Deployment progress tracker
  - Receipt download button

### Figma Design Tips
- Use a light background to emphasize security
- Add lock icons and trust badges
- Success animation: Use Lottie plugin for checkmark
- Keep payment form minimal and clean
- Mobile: Single column layout

---

## 6. Message Detail Screen

### Purpose
View details of a specific message (pending or delivered).

### Layout - Pending Message
```
┌─────────────────────────────────────────────────┐
│  ← Messages        Message Details           ⋮  │
├─────────────────────────────────────────────────┤
│                                                  │
│  🎂 Birthday Message for Sarah                  │
│                                                  │
│  ┌──────────────────────────────────────────┐  │
│  │ Unlock Status                             │  │
│  │                                           │  │
│  │     ⏰ Unlocks in 28 days                 │  │
│  │                                           │  │
│  │     December 15, 2024 at 2:00 PM PST     │  │
│  │                                           │  │
│  │     [████████████████░░░░░░] 70%         │  │
│  │     Deploying to blockchains...           │  │
│  └──────────────────────────────────────────┘  │
│                                                  │
│  ┌──────────────────────────────────────────┐  │
│  │ Deployment Details                        │  │
│  │                                           │  │
│  │ Tier: ⭐ Standard                         │  │
│  │ Encryption: AES-256-GCM                   │  │
│  │ Secret Sharing: 3-of-5 Shamir            │  │
│  │                                           │  │
│  │ Blockchain Distribution:                  │  │
│  │ ✅ Bitcoin     (Block #812,345)          │  │
│  │ ✅ Ethereum    (Block #18,234,567)       │  │
│  │ ✅ Polygon     (Block #52,345,678)       │  │
│  │ ⏳ Arbitrum    (Deploying...)            │  │
│  │ ⏳ Base        (Pending...)              │  │
│  │                                           │  │
│  │ Message ID: msg_7h2j8k9l0m1n2o3p         │  │
│  │ Created: Nov 17, 2024 at 3:45 PM         │  │
│  └──────────────────────────────────────────┘  │
│                                                  │
│  ┌──────────────────────────────────────────┐  │
│  │ Recipient                                 │  │
│  │ sarah@example.com                         │  │
│  │ Will receive email when message unlocks   │  │
│  └──────────────────────────────────────────┘  │
│                                                  │
│  [Cancel Message]  [Edit Recipient]  [Share]   │
│                                                  │
└─────────────────────────────────────────────────┘
```

### Layout - Delivered Message
```
┌─────────────────────────────────────────────────┐
│  ← Messages        Message Details           ⋮  │
├─────────────────────────────────────────────────┘
│                                                  │
│  💭 Dear Future Me - 2024 Goals                 │
│                                                  │
│  ┌──────────────────────────────────────────┐  │
│  │ ✅ Unlocked Jan 1, 2024 at 12:00 AM      │  │
│  └──────────────────────────────────────────┘  │
│                                                  │
│  ┌──────────────────────────────────────────┐  │
│  │ Message Content                           │  │
│  │                                           │  │
│  │ Dear 2024 Me,                             │  │
│  │                                           │  │
│  │ I hope you've achieved all the goals      │  │
│  │ we set this year! Remember:               │  │
│  │                                           │  │
│  │ • Launch the side project                 │  │
│  │ • Travel to Japan                         │  │
│  │ • Read 24 books                           │  │
│  │                                           │  │
│  │ Stay awesome!                             │  │
│  │                                           │  │
│  │ - 2023 You                                │  │
│  └──────────────────────────────────────────┘  │
│                                                  │
│  ┌──────────────────────────────────────────┐  │
│  │ 📎 Attachments (2)                        │  │
│  │ • goals-checklist.pdf (234 KB)           │  │
│  │ • motivation-photo.jpg (1.2 MB)          │  │
│  └──────────────────────────────────────────┘  │
│                                                  │
│  [Download All] [Reply to Past Self] [Delete]  │
│                                                  │
└─────────────────────────────────────────────────┘
```

### Elements
- **Status Card**
  - Countdown timer (for pending) or unlock confirmation (for delivered)
  - Progress bar for deployment status
  - Target date/time display

- **Technical Details**
  - Deployment tier badge
  - Blockchain deployment status with checkmarks
  - Message ID (for support)
  - Encryption details

- **Message Content** (delivered only)
  - Full message text with formatting
  - Attachments list with download buttons

- **Actions**
  - Pending: Cancel, Edit Recipient, Share status
  - Delivered: Download, Reply, Delete

### Figma Design Tips
- Use status colors: Green (delivered), Orange (pending), Red (cancelled)
- Blockchain list: Icons for each chain, checkmarks for deployed
- Add subtle animations for progress bars
- Message content: Use a serif font for better readability
- Action buttons: Distinguish destructive actions (red for delete/cancel)

---

## 7. Settings Screen

### Purpose
User account management, preferences, and billing.

### Sections

**Profile Settings**
```
┌──────────────────────────────────────────┐
│ Profile                                   │
│                                           │
│ [Avatar]  Change Photo                   │
│                                           │
│ Name:     [John Doe]                     │
│ Email:    [john@example.com]             │
│                                           │
│ [Save Changes]                           │
└──────────────────────────────────────────┘
```

**Notification Preferences**
```
┌──────────────────────────────────────────┐
│ Notifications                             │
│                                           │
│ [✓] Email me when a message unlocks      │
│ [✓] Deployment status updates            │
│ [✓] Weekly summary of upcoming messages   │
│ [ ] Marketing emails                      │
│                                           │
│ [Save Preferences]                       │
└──────────────────────────────────────────┘
```

**Billing & Usage**
```
┌──────────────────────────────────────────┐
│ Usage This Month                          │
│                                           │
│ Messages Created: 5                       │
│ Total Spent: $12.50                      │
│                                           │
│ Payment Methods                           │
│ • Visa •••• 4242  [Default] [Remove]     │
│ [+ Add Payment Method]                   │
│                                           │
│ Transaction History                       │
│ Nov 17 - Standard Message     $3.00      │
│ Nov 12 - Premium Message     $13.00      │
│ [View All]                               │
└──────────────────────────────────────────┘
```

**Security**
```
┌──────────────────────────────────────────┐
│ Security                                  │
│                                           │
│ Two-Factor Authentication                │
│ [Enable 2FA]                             │
│                                           │
│ Active Sessions                           │
│ • Chrome on MacOS (Current)              │
│ • Safari on iPhone (2 hours ago)         │
│                                           │
│ [Change Password]                        │
│ [View Security Log]                      │
└──────────────────────────────────────────┘
```

### Figma Design Tips
- Use a sidebar navigation for desktop, tabs for mobile
- Form inputs: 48px height for accessibility
- Toggle switches for preferences (use component variants)
- Transaction history: Table/list format with subtle hover states
- Desktop: Two-column layout (nav + content)

---

## 8. Empty States

### No Messages Yet
```
┌─────────────────────────────────────────────────┐
│                                                  │
│              📭                                  │
│                                                  │
│        No messages yet                          │
│                                                  │
│    Create your first time-locked message        │
│    and start your journey!                      │
│                                                  │
│    [Create Your First Message]                  │
│                                                  │
└─────────────────────────────────────────────────┘
```

### No Upcoming Messages
```
┌─────────────────────────────────────────────────┐
│                                                  │
│              ✨                                  │
│                                                  │
│        All caught up!                           │
│                                                  │
│    You have no messages unlocking soon.         │
│                                                  │
│    [Create New Message]                         │
│                                                  │
└─────────────────────────────────────────────────┘
```

---

## 9. Mobile Considerations

### Key Adaptations
1. **Bottom Navigation** (instead of top)
   - Home, Create, Messages, Profile

2. **Swipe Gestures**
   - Swipe right on message card → View details
   - Swipe left → Quick delete/cancel

3. **FAB (Floating Action Button)**
   - Always-visible "+" button for creating messages

4. **Touch Targets**
   - Minimum 44x44px for all interactive elements

5. **Simplified Forms**
   - Break multi-step forms into full screens
   - Use native date/time pickers

---

## Design System Guidelines for Figma

### Colors
```
Primary Purple:   #6366F1 (Indigo-500)
Primary Hover:    #4F46E5 (Indigo-600)
Secondary Blue:   #3B82F6 (Blue-500)
Success Green:    #10B981 (Emerald-500)
Warning Orange:   #F59E0B (Amber-500)
Error Red:        #EF4444 (Red-500)

Background:       #FFFFFF (White)
Surface:          #F9FAFB (Gray-50)
Border:           #E5E7EB (Gray-200)

Text Primary:     #111827 (Gray-900)
Text Secondary:   #6B7280 (Gray-500)
Text Disabled:    #9CA3AF (Gray-400)
```

### Typography
```
Heading 1: Inter Bold, 48px, Line: 56px
Heading 2: Inter Bold, 36px, Line: 44px
Heading 3: Inter Semibold, 24px, Line: 32px
Heading 4: Inter Semibold, 20px, Line: 28px

Body Large: Inter Regular, 18px, Line: 28px
Body: Inter Regular, 16px, Line: 24px
Body Small: Inter Regular, 14px, Line: 20px

Caption: Inter Medium, 12px, Line: 16px
Button: Inter Semibold, 16px
```

### Spacing Scale (8px grid)
```
xs:  4px
sm:  8px
md:  16px
lg:  24px
xl:  32px
2xl: 48px
3xl: 64px
```

### Border Radius
```
Small:  4px (inputs, chips)
Medium: 8px (cards, buttons)
Large:  16px (modals, major sections)
Full:   999px (pills, avatars)
```

### Shadows
```
Small:  0px 1px 2px rgba(0, 0, 0, 0.05)
Medium: 0px 4px 6px rgba(0, 0, 0, 0.07)
Large:  0px 10px 15px rgba(0, 0, 0, 0.1)
XLarge: 0px 20px 25px rgba(0, 0, 0, 0.15)
```

---

## Figma Setup Instructions

### 1. Create Design System First
- Set up color styles (right panel → "+" → Color Style)
- Create text styles for all typography variants
- Build component library:
  - Buttons (Primary, Secondary, Destructive - with hover states)
  - Input fields (Default, Focus, Error states)
  - Cards (Message Card, Stat Card, etc.)
  - Navigation (Top Nav, Tabs, Bottom Nav)
  - Icons (use Heroicons or Feather icons)

### 2. Create Layouts
- Desktop: 1440px artboard
- Tablet: 768px artboard
- Mobile: 375px artboard

### 3. Use Auto Layout Everywhere
- Makes responsive design much easier
- Set constraints properly (Left/Right for horizontal, Top/Bottom for vertical)

### 4. Create Interactive Prototype
- Link all screens together
- Add Smart Animate for smooth transitions
- Set up hover states on buttons
- Create micro-interactions (button press, card hover)

### 5. Organize Layers
- Name layers clearly: "Card / Message / Title"
- Use frames for sections
- Group related elements
- Lock background elements to prevent accidental moves

### 6. Version Control
- Save versions before major changes: "v1.0 - Initial Screens"
- Use Figma Branching for experimentation

### 7. Plugins to Use
- **Unsplash**: For placeholder images
- **Iconify**: Access to thousands of icons
- **Stark**: Accessibility checker (contrast ratios)
- **Lorem Ipsum**: Generate placeholder text
- **Anima**: Export to React code (optional)

---

## Additional Screen Ideas

### Future Enhancements

1. **Message Templates Screen**
   - Pre-written templates for common use cases
   - "Birthday wishes", "New Year's resolutions", "Anniversary messages"

2. **Sharing Screen**
   - Generate shareable links to sent messages (public view)
   - Social sharing with preview cards

3. **Analytics Dashboard** (for power users)
   - Message delivery rates
   - Most popular unlock dates
   - Storage usage

4. **Collaboration Features**
   - Group messages (multiple contributors, one unlock date)
   - Shared time capsules

---

## Accessibility Notes

- **Contrast**: All text must meet WCAG AA standards (4.5:1 for body, 3:1 for large text)
- **Keyboard Navigation**: All interactive elements must be keyboard-accessible
- **Screen Readers**: Use semantic HTML, proper ARIA labels
- **Focus States**: Clear visual indicators for keyboard focus
- **Alternative Text**: All images and icons need alt text

---

## Next Steps

1. Review this proposal and gather feedback
2. Create low-fidelity wireframes in Figma
3. Build design system components
4. Design high-fidelity mockups for all screens
5. Create interactive prototype for user testing
6. Iterate based on feedback
7. Prepare design handoff for developers (with Zeplin or Figma Inspect)

---

## Questions to Consider

Before starting design:

1. **Branding**: Do you have a logo? Color preferences?
2. **Target Audience**: Age range? Tech-savviness?
3. **Platform Priority**: Web app first? Native mobile?
4. **Competitors**: Any apps you want to reference for inspiration?
5. **Timeline**: When do you need designs completed?

Let me know your thoughts and preferences, and I can refine this proposal further!
